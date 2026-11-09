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

sealed interface UsersUiState {
    data object Loading : UsersUiState
    data class Success(val people: PeopleSnapshot) : UsersUiState
    data class Error(val message: String) : UsersUiState
}

class UsersViewModel(private val repository: PeopleRepository) : ViewModel() {
    private val groupRepository = GroupRepository()
    private var listener: ListenerRegistration? = null
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

    init { loadUsers(); groupsListener = groupRepository.observeGroups { it.onSuccess { list -> groups = list }.onFailure { statusMessage = it.localizedMessage } } }

    fun searchUsername(value: String) {
        val token = ++searchGeneration
        if (value.trim().removePrefix("@").length < 2) { searchResults = emptyList(); searchFinished = false; return }
        searchFinished = false
        repository.searchByUsername(value) { result ->
            if (token != searchGeneration) return@searchByUsername
            searchResults = result.getOrNull().orEmpty()
            searchFinished = true
            result.exceptionOrNull()?.let { showStatus(it.localizedMessage ?: "Username search failed") }
        }
    }

    fun clearSearch() { searchGeneration++; searchResults = emptyList(); searchFinished = false }
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
                onSuccess = { UsersUiState.Success(it) },
                onFailure = { UsersUiState.Error(it.localizedMessage ?: "Could not load users") },
            )
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
        listener?.remove()
        listener = null
    }

    fun stopGroupObserving() { groupsListener?.remove(); groupsListener = null }

    override fun onCleared() {
        stopObserving()
        stopGroupObserving()
        super.onCleared()
    }
}
