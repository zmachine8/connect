package ee.ut.connect.ui.users

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import ee.ut.connect.data.model.ConnectUser
import ee.ut.connect.data.repository.ConnectGroup
import ee.ut.connect.data.repository.ContactRequest
import ee.ut.connect.data.repository.PeopleRepository
import ee.ut.connect.data.repository.PeopleSnapshot
import ee.ut.connect.ui.theme.AdaptiveContent
import ee.ut.connect.ui.theme.CopperDivider
import ee.ut.connect.ui.theme.FantasyBackdrop
import ee.ut.connect.ui.theme.InitialAvatar

private enum class PeopleSection(val title: String) { CONNECTIONS("Connections"), GROUPS("Groups"), ACCESS("Access") }
private enum class AccessSection(val title: String) { REQUESTS("Requests"), BLOCKED("Blocked") }

@Composable
fun UsersRoute(
    onUserSelected: (ConnectUser) -> Unit,
    onGroupSelected: (ConnectGroup) -> Unit,
    onSignOut: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: UsersViewModel = viewModel { UsersViewModel(PeopleRepository()) },
) {
    UsersScreen(
        state = viewModel.uiState, groups = viewModel.groups,
        searchResults = viewModel.searchResults, searchFinished = viewModel.searchFinished,
        statusMessage = viewModel.statusMessage,
        onUserSelected = onUserSelected, onGroupSelected = onGroupSelected,
        onSearch = viewModel::searchUsername, onClearSearch = viewModel::clearSearch,
        onRetry = viewModel::loadUsers, onOpenProfile = onOpenProfile,
        onSendRequest = viewModel::sendRequest, onRespond = viewModel::respond,
        onBlock = { viewModel.setBlocked(it, true) }, onUnblock = { viewModel.setBlocked(it, false) },
        onCreateGroup = viewModel::createGroup,
        onAddToGroup = viewModel::addToGroup,
        onClearMessage = viewModel::clearStatusMessage,
        onSignOut = { viewModel.stopObserving(); viewModel.stopGroupObserving(); onSignOut() },
    )
}

@Composable
fun UsersScreen(
    state: UsersUiState,
    groups: List<ConnectGroup>,
    searchResults: List<ConnectUser>,
    searchFinished: Boolean,
    statusMessage: String?,
    onUserSelected: (ConnectUser) -> Unit,
    onGroupSelected: (ConnectGroup) -> Unit,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onRetry: () -> Unit,
    onOpenProfile: () -> Unit,
    onSendRequest: (ConnectUser) -> Unit,
    onRespond: (ContactRequest, Boolean) -> Unit,
    onBlock: (String) -> Unit,
    onUnblock: (String) -> Unit,
    onCreateGroup: (String) -> Unit,
    onAddToGroup: (ConnectGroup, ConnectUser) -> Unit,
    onClearMessage: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    var section by remember { mutableStateOf(PeopleSection.CONNECTIONS) }
    var accessSection by remember { mutableStateOf(AccessSection.REQUESTS) }
    var query by remember { mutableStateOf("") }
    var createDialog by remember { mutableStateOf(false) }
    LaunchedEffect(statusMessage) {
        if (statusMessage != null) { delay(3000); onClearMessage() }
    }
    FantasyBackdrop {
        AdaptiveContent(modifier) { compact ->
            Column(Modifier.align(Alignment.TopCenter).widthIn(max = 840.dp).fillMaxSize()
                .padding(if (compact) 8.dp else 20.dp)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = { focusManager.clearFocus() },
                )) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("People", style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    TextButton(onClick = { focusManager.clearFocus(); onOpenProfile() }) { Text("Profile") }
                    TextButton(onClick = { focusManager.clearFocus(); onSignOut() }) { Text("Sign out") }
                }
                CopperDivider(Modifier.padding(top = if (compact) 0.dp else 8.dp, bottom = if (compact) 4.dp else 12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Find people", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = {
                        focusManager.clearFocus()
                        val displayName = (state as? UsersUiState.Success)?.people?.myDisplayName ?: "Connect user"
                        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT,
                            "Join me on Connect. Install Connect, create an account, and search for my username in the People screen.") }
                        context.startActivity(Intent.createChooser(intent, "Invite someone to Connect"))
                    }) { Text("Invite") }
                }
                OutlinedTextField(value = query, onValueChange = { query = it; if (it.isBlank()) onClearSearch() else onSearch(it) },
                    label = { Text("Search by username") }, placeholder = { Text("Search all users by username") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp))
                val navPeople = (state as? UsersUiState.Success)?.people
                val connectionUnread = navPeople?.users.orEmpty().filter { it.id in navPeople?.connectionIds.orEmpty() }.sumOf { it.unreadCount }
                val groupUnread = groups.sumOf { it.unreadCount }
                val requestCount = navPeople?.incomingRequests?.size ?: 0
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    PeopleSection.entries.forEach { item ->
                        val count = when (item) {
                            PeopleSection.CONNECTIONS -> connectionUnread
                            PeopleSection.GROUPS -> groupUnread
                            PeopleSection.ACCESS -> requestCount
                        }
                        TextButton(onClick = { focusManager.clearFocus(); section = item; query = ""; onClearSearch() }, modifier = Modifier.weight(1f)) {
                            Text(item.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (section == item) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (section == item) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1, softWrap = false)
                            if (count > 0) { Spacer(Modifier.width(4.dp)); UnreadBadge(count) }
                        }
                    }
                }
                if (section == PeopleSection.ACCESS && query.isBlank()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        AccessSection.entries.forEachIndexed { index, item ->
                            if (index > 0) Text("|", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { accessSection = item }) {
                                Text(item.title,
                                    fontWeight = if (accessSection == item) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (accessSection == item) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
                when (val current = state) {
                    UsersUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    is UsersUiState.Error -> Column { Text(current.message, color = MaterialTheme.colorScheme.error); Button(onClick = onRetry) { Text("Try again") } }
                    is UsersUiState.Success -> {
                        if (statusMessage != null) TextButton(onClick = onClearMessage) { Text(statusMessage) }
                        val people = current.people
                        val connected = people.users.filter { it.id in people.connectionIds }
                        if (query.isNotBlank()) {
                            when {
                                query.trim().removePrefix("@").length < 2 -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("Enter at least 2 username characters. Search includes all registered users.")
                                }
                                !searchFinished -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                                searchResults.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("No users found with usernames starting with that text.")
                                }
                                else -> LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(searchResults.sortedWith(compareByDescending<ConnectUser> { it.id in people.connectionIds }
                                        .thenBy { it.username.lowercase() }), key = { "search-${it.id}" }) { user ->
                                        SearchUserRow(user, user.id in people.connectionIds, user.id in people.outgoingRequestIds,
                                            groups.filter { it.createdBy == currentUid }, onUserSelected, onSendRequest, onBlock,
                                            { group -> onAddToGroup(group, user) })
                                    }
                                }
                            }
                        } else when (section) {
                            PeopleSection.CONNECTIONS -> if (connected.isEmpty()) EmptyText("No connections yet. Search for someone by username to connect.")
                                else LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(connected, key = { it.id }) { user ->
                                        SearchUserRow(user, true, false, groups.filter { it.createdBy == currentUid },
                                            onUserSelected, onSendRequest, onBlock, { group -> onAddToGroup(group, user) })
                                    }
                                }
                            PeopleSection.GROUPS -> Box(Modifier.weight(1f).fillMaxWidth()) {
                                if (groups.isEmpty()) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        EmptyText("You are not in any groups yet.")
                                    }
                                } else {
                                    LazyColumn(
                                        Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(bottom = 88.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        items(groups, key = { it.id }) { group ->
                                            Surface(onClick = { onGroupSelected(group) }, shape = CutCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = .96f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .6f))) {
                                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Text(group.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                                    if (group.unreadCount > 0) UnreadBadge(group.unreadCount)
                                                    Text("${group.memberIds.size} members", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                                Button(
                                    onClick = { createDialog = true },
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                                ) { Text("Create group") }
                            }
                            PeopleSection.ACCESS -> when (accessSection) {
                                AccessSection.REQUESTS -> if (people.incomingRequests.isEmpty()) EmptyText("No pending connection requests")
                                else LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(people.incomingRequests, key = { it.id }) { RequestRow(it, onRespond) }
                                }
                                AccessSection.BLOCKED -> if (people.blockedUsers.isEmpty()) EmptyText("No blocked people")
                                else LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(people.blockedUsers, key = { it.id }) { user -> UserRow(user, false, false, true, onUserSelected, onSendRequest, onBlock, onUnblock) }
                                }
                            }
                        }
                        if (createDialog) CreateGroupDialog(onDismiss = { createDialog = false }, onCreate = { name ->
                            createDialog = false; onCreateGroup(name)
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun UserRow(user: ConnectUser, connected: Boolean, pending: Boolean, blocked: Boolean,
                    onMessage: (ConnectUser) -> Unit, onConnect: (ConnectUser) -> Unit, onBlock: (String) -> Unit,
                    onUnblock: (String) -> Unit = {}) {
    Surface(shape = CutCornerShape(8.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .6f))) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InitialAvatar(user.displayName, Modifier.size(42.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(user.displayName, modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    PresenceLabel(user.isOnline)
                    if (user.unreadCount > 0) UnreadBadge(user.unreadCount)
                }
                if (user.username.isNotBlank()) Text("@${user.username}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when { blocked -> TextButton(onClick = { onUnblock(user.id) }) { Text("Unblock") }
                connected -> Button(onClick = { onMessage(user) }) { Text("Message") }
                pending -> Text("Pending", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> OutlinedButton(onClick = { onConnect(user) }) { Text("Connect") } }
            if (!blocked) TextButton(onClick = { onBlock(user.id) }) { Text("Block") }
        }
    }
}

@Composable
private fun SearchUserRow(
    user: ConnectUser,
    connected: Boolean,
    pending: Boolean,
    groups: List<ConnectGroup>,
    onMessage: (ConnectUser) -> Unit,
    onConnect: (ConnectUser) -> Unit,
    onBlock: (String) -> Unit,
    onAddToGroup: (ConnectGroup) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var chooseGroup by remember { mutableStateOf(false) }
    Surface(shape = CutCornerShape(8.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .6f))) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InitialAvatar(user.displayName, Modifier.size(42.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(user.displayName, modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    PresenceLabel(user.isOnline)
                    if (user.unreadCount > 0) UnreadBadge(user.unreadCount)
                }
                if (user.username.isNotBlank()) Text("@${user.username}", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                TextButton(onClick = { menuExpanded = true }) { Text("⋮") }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    if (connected) DropdownMenuItem(text = { Text("Message") }, onClick = { menuExpanded = false; onMessage(user) })
                    else if (pending) DropdownMenuItem(text = { Text("Request pending") }, enabled = false, onClick = {})
                    else DropdownMenuItem(text = { Text("Connect") }, onClick = { menuExpanded = false; onConnect(user) })
                    DropdownMenuItem(text = { Text("Add to group") }, enabled = groups.isNotEmpty(), onClick = {
                        menuExpanded = false; chooseGroup = groups.isNotEmpty()
                    })
                    DropdownMenuItem(text = { Text("Block") }, onClick = { menuExpanded = false; onBlock(user.id) })
                }
            }
        }
    }
    if (chooseGroup) AlertDialog(
        onDismissRequest = { chooseGroup = false },
        title = { Text("Add @${user.username} to a group") },
        text = { Column {
            if (groups.isEmpty()) Text("Create a group first.")
            groups.forEach { group ->
                TextButton(onClick = { chooseGroup = false; onAddToGroup(group) }, modifier = Modifier.fillMaxWidth()) {
                    Text(group.name, modifier = Modifier.fillMaxWidth())
                }
            }
        } },
        confirmButton = { TextButton(onClick = { chooseGroup = false }) { Text("Close") } },
    )
}

@Composable
private fun PresenceLabel(isOnline: Boolean?) {
    Text(
        text = when (isOnline) { true -> "Online"; false -> "Offline"; null -> "Unavailable" },
        style = MaterialTheme.typography.labelSmall,
        color = when (isOnline) {
            true -> Color(0xFF83D18A)
            false -> Color(0xFFE88A82)
            null -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        maxLines = 1,
    )
}

@Composable
private fun UnreadBadge(count: Int) {
    if (count > 0) {
        Box(Modifier.padding(horizontal = 3.dp).size(8.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .semantics { contentDescription = "Unread" })
    }
}

@Composable private fun EmptyText(text: String) = Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) { Text(text) }

@Composable
private fun RequestRow(request: ContactRequest, onRespond: (ContactRequest, Boolean) -> Unit) {
    Surface(shape = CutCornerShape(8.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .6f))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InitialAvatar(request.senderName, Modifier.size(44.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(request.senderName, modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    PresenceLabel(request.isOnline)
                }
            }
            Button(onClick = { onRespond(request, true) }) { Text("Accept") }
            TextButton(onClick = { onRespond(request, false) }) { Text("Decline") }
        }
    }
}

@Composable
private fun CreateGroupDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Create group") },
        text = { Column {
            OutlinedTextField(value = name, onValueChange = { name = it.take(60) }, label = { Text("Group name") }, singleLine = true)
            Text("You can add people after creating the group.", modifier = Modifier.padding(top = 12.dp))
        } }, confirmButton = { Button(onClick = { onCreate(name) }, enabled = name.trim().length >= 2) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
