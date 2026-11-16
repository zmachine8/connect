package ee.ut.connect.ui.users

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.firebase.firestore.ListenerRegistration
import ee.ut.connect.data.model.ConnectUser
import ee.ut.connect.data.repository.ContactRequest
import ee.ut.connect.data.repository.PeopleRepository
import ee.ut.connect.data.repository.PeopleSnapshot
import ee.ut.connect.data.repository.GroupRepository
import ee.ut.connect.data.repository.ConnectGroup
import ee.ut.connect.data.repository.PresenceRepository
import ee.ut.connect.data.repository.PresenceSubscription
import ee.ut.connect.data.repository.UnreadRepository
import ee.ut.connect.data.repository.UnreadMarker
import ee.ut.connect.data.repository.LatestConversationMessage

sealed interface UsersUiState {
    data object Loading : UsersUiState
    data class Success(val people: PeopleSnapshot) : UsersUiState
    data class Error(val message: String) : UsersUiState
}

class UsersViewModel(private val repository: PeopleRepository) : ViewModel() {
    private val groupRepository = GroupRepository()
    private val presenceRepository = PresenceRepository()
    private val unreadRepository = UnreadRepository()
    private var unreadListener: ListenerRegistration? = null
    private var unreadMarkers: Map<String, UnreadMarker> = emptyMap()
    private var latestMessages: Map<String, LatestConversationMessage?> = emptyMap()
    private val latestMessageListeners = mutableMapOf<String, ListenerRegistration>()
    private var connectionUserIds: Set<String> = emptySet()
    private var listener: ListenerRegistration? = null
    private var peoplePresence: PresenceSubscription? = null
    private var searchPresence: PresenceSubscription? = null
    private var peoplePresenceGeneration = 0
    private var observing = false
    private var generation = 0
    var uiState: UsersUiState by mutableStateOf(UsersUiState.Loading)
        private set
    var searchResults: List<ConnectUser> by mutableStateOf(emptyList())
        private set
    var searchFinished by mutableStateOf(false)
        private set
    var groups by mutableStateOf<List<ConnectGroup>>(emptyList())
        private set
    private var groupsListener: ListenerRegistration? = null
    private var searchGeneration = 0

    init {
        loadUsers()
        groupsListener = groupRepository.observeGroups { result ->
            result.onSuccess { list ->
                groups = list
                syncLatestMessageListeners()
                refreshUnreadBadges()
            }.onFailure { statusMessage = it.localizedMessage }
        }
        unreadListener = unreadRepository.observe { markers ->
            unreadMarkers = markers
            refreshUnreadBadges()
        }
    }

    private fun directUnreadKey(otherId: String): String {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        return unreadRepository.directKey(listOf(uid, otherId).sorted().joinToString("_"))
    }
    private fun hasUnread(key: String): Boolean {
        val marker = unreadMarkers[key]
        if ((marker?.count ?: 0) > 0) return true
        val latest = latestMessages[key] ?: return false
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        return latest.senderId != uid && latest.createdAtMillis > (marker?.readAtMillis ?: 0L)
    }

    private fun withDirectUnread(users: List<ConnectUser>) = users.map { user ->
        user.copy(unreadCount = if (hasUnread(directUnreadKey(user.id))) 1 else 0)
    }
    private fun withGroupUnread(items: List<ConnectGroup>) = items.map { group ->
        group.copy(unreadCount = if (hasUnread(unreadRepository.groupKey(group.id))) 1 else 0)
    }

    private fun refreshUnreadBadges() {
        groups = withGroupUnread(groups)
        searchResults = withDirectUnread(searchResults)
        val current = uiState as? UsersUiState.Success ?: return
        uiState = UsersUiState.Success(current.people.copy(
            users = withDirectUnread(current.people.users),
            blockedUsers = withDirectUnread(current.people.blockedUsers),
        ))
    }

    private fun syncLatestMessageListeners() {
        val wanted = mutableMapOf<String, Pair<Boolean, String>>()
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        connectionUserIds.forEach { otherId ->
            val chatId = listOf(uid, otherId).sorted().joinToString("_")
            wanted[unreadRepository.directKey(chatId)] = true to chatId
        }
        groups.forEach { group -> wanted[unreadRepository.groupKey(group.id)] = false to group.id }

        (latestMessageListeners.keys - wanted.keys).forEach { key ->
            latestMessageListeners.remove(key)?.remove()
            latestMessages = latestMessages - key
        }
        wanted.forEach { (key, conversation) ->
            if (key !in latestMessageListeners) {
                latestMessageListeners[key] = if (conversation.first) {
                    unreadRepository.observeLatestDirect(conversation.second) { latest ->
                        latestMessages = latestMessages + (key to latest)
                        refreshUnreadBadges()
                    }
                } else {
                    unreadRepository.observeLatestGroup(conversation.second) { latest ->
                        latestMessages = latestMessages + (key to latest)
                        refreshUnreadBadges()
                    }
                }
            }
        }
    }

    fun searchUsername(value: String) {
        val token = ++searchGeneration
        searchPresence?.remove()
        searchPresence = null
        if (value.trim().removePrefix("@").length < 2) { searchResults = emptyList(); searchFinished = false; return }
        searchFinished = false
        repository.searchByUsername(value) { result ->
            if (token != searchGeneration) return@searchByUsername
            val results = result.getOrNull().orEmpty()
            searchResults = withDirectUnread(results)
            searchFinished = true
            searchPresence = presenceRepository.observe(results.map { it.id }) { states ->
                if (token == searchGeneration) {
                    searchResults = withDirectUnread(results.map { it.copy(isOnline = states[it.id]) })
                }
            }
            result.exceptionOrNull()?.let { showStatus(it.localizedMessage ?: "Username search failed") }
        }
    }

    fun clearSearch() {
        searchGeneration++
        searchPresence?.remove()
        searchPresence = null
        searchResults = emptyList()
        searchFinished = false
    }
    fun createGroup(name: String) {
        groupRepository.createGroup(name) { result ->
            showStatus(result.fold({ "Group created" }, { it.localizedMessage ?: "Could not create group" }))
        }
    }

    fun addToGroup(group: ConnectGroup, user: ConnectUser) {
        groupRepository.addMember(group.id, user.id) { result ->
            showStatus(result.fold({ "Added @${user.username} to ${group.name}" }, { it.localizedMessage ?: "Could not add person to group" }))
        }
    }

    fun loadUsers() {
        stopObserving()
        observing = true
        val currentGeneration = generation
        uiState = UsersUiState.Loading
        listener = repository.observePeople { result ->
            if (!observing || generation != currentGeneration) return@observePeople
            uiState = result.fold(
                onSuccess = {
                    connectionUserIds = it.users.filter { user -> user.id in it.connectionIds }.map { user -> user.id }.toSet()
                    observePeoplePresence(it)
                    syncLatestMessageListeners()
                    UsersUiState.Success(it.copy(users = withDirectUnread(it.users), blockedUsers = withDirectUnread(it.blockedUsers)))
                },
                onFailure = { UsersUiState.Error(it.localizedMessage ?: "Could not load users") },
            )
        }
    }

    private fun observePeoplePresence(snapshot: PeopleSnapshot) {
        val token = ++peoplePresenceGeneration
        peoplePresence?.remove()
        val peopleIds = (snapshot.users + snapshot.blockedUsers).map { it.id } + snapshot.incomingRequests.map { it.senderId }
        peoplePresence = presenceRepository.observe(peopleIds) { states ->
            if (!observing || token != peoplePresenceGeneration) return@observe
            uiState = UsersUiState.Success(snapshot.copy(
                users = withDirectUnread(snapshot.users.map { it.copy(isOnline = states[it.id]) }),
                blockedUsers = withDirectUnread(snapshot.blockedUsers.map { it.copy(isOnline = states[it.id]) }),
                incomingRequests = snapshot.incomingRequests.map { it.copy(isOnline = states[it.senderId]) },
            ))
        }
    }

    fun sendRequest(user: ConnectUser) {
        repository.sendRequest(user) { result ->
            showStatus(result.fold({ "Request sent" }, { it.localizedMessage ?: "Could not send request" }))
        }
    }

    fun respond(request: ContactRequest, accept: Boolean) {
        repository.respondToRequest(request, accept) { result ->
            showStatus(result.fold(
                { if (accept) "Connection added" else "Request declined" },
                { it.localizedMessage ?: "Could not update request" },
            ))
        }
    }

    fun setBlocked(userId: String, blocked: Boolean) {
        repository.setBlocked(userId, blocked) { result ->
            showStatus(result.fold(
                { if (blocked) "Person blocked" else "Person unblocked" },
                { it.localizedMessage ?: "Could not update block list" },
            ))
        }
    }

    var statusMessage: String? by mutableStateOf(null)
        private set
    private var statusGeneration = 0
    private fun showStatus(value: String) {
        val token = ++statusGeneration
        statusMessage = value
        viewModelScope.launch { delay(3000); if (token == statusGeneration) statusMessage = null }
    }
    fun clearStatusMessage() { statusGeneration++; statusMessage = null }

    fun stopObserving() {
        observing = false
        generation++
        peoplePresenceGeneration++
        listener?.remove()
        listener = null
        peoplePresence?.remove()
        peoplePresence = null
        searchPresence?.remove()
        searchPresence = null
        latestMessageListeners.values.forEach { it.remove() }
        latestMessageListeners.clear()
        latestMessages = emptyMap()
        connectionUserIds = emptySet()
    }

    fun stopGroupObserving() {
        groupsListener?.remove(); groupsListener = null
        unreadListener?.remove(); unreadListener = null
        latestMessageListeners.values.forEach { it.remove() }
        latestMessageListeners.clear()
        latestMessages = emptyMap()
    }

    override fun onCleared() {
        stopObserving()
        stopGroupObserving()
        super.onCleared()
    }
}
