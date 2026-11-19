package ee.ut.connect.ui.users

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import ee.ut.connect.data.repository.GroupMessage
import ee.ut.connect.data.repository.GroupRepository
import ee.ut.connect.ui.theme.AdaptiveContent
import ee.ut.connect.ui.theme.FantasyBackdrop

@Composable
fun GroupChatRoute(groupId: String, groupName: String, onBack: () -> Unit) {
    val repository = remember { GroupRepository() }
    var messages by remember(groupId) { mutableStateOf<List<GroupMessage>>(emptyList()) }
    var draft by remember(groupId) { mutableStateOf("") }
    var loading by remember(groupId) { mutableStateOf(true) }
    var sending by remember(groupId) { mutableStateOf(false) }
    var loadError by remember(groupId) { mutableStateOf<String?>(null) }
    var sendError by remember(groupId) { mutableStateOf<String?>(null) }
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    DisposableEffect(groupId) {
        val listener = repository.observeMessages(groupId) { result ->
            loading = false
            result.onSuccess { loadedMessages ->
                messages = loadedMessages
                loadError = null
                repository.markRead(groupId)
            }.onFailure {
                loadError = "Could not load group messages."
            }
        }
        onDispose { listener.remove() }
    }
    FantasyBackdrop {
        AdaptiveContent { compact ->
            Column(Modifier.align(Alignment.TopCenter).widthIn(max = 800.dp).fillMaxSize().padding(if (compact) 12.dp else 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) { Text("Back") }
                    Text(groupName, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                }

                LazyColumn(Modifier.weight(1f).fillMaxWidth(), reverseLayout = false, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        loading -> item {
                            CircularProgressIndicator()
                        }
                        loadError != null -> item {
                            Text(loadError.orEmpty(), color = MaterialTheme.colorScheme.error)
                        }
                        messages.isEmpty() -> item {
                            Text("No messages yet. Send the first message.")
                        }
                    }

                    items(messages, key = { it.id }) { message ->
                        Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = .94f), shape = MaterialTheme.shapes.medium) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(if (message.senderId == uid) "You" else message.senderName, color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium)
                                Text(message.text)
                            }
                        }
                    }
                }

                sendError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = draft, onValueChange = { draft = it.take(2000) }, label = { Text("Message group") },
                        modifier = Modifier.weight(1f), maxLines = 4)
                    Button(
                        onClick = {
                            if (!loading && loadError == null && !sending && draft.isNotBlank()) {
                                val submittedDraft = draft
                                sending = true
                                sendError = null

                                repository.sendMessage(groupId, submittedDraft) { result ->
                                    sending = false
                                    result.onSuccess {
                                        if (draft == submittedDraft) draft = ""
                                    }.onFailure {
                                        sendError = "Could not send the message. Please try again."
                                    }
                                }
                            }
                        },
                        enabled = !loading && loadError == null && !sending && draft.isNotBlank(),
                    ) {
                        Text(if (sending) "Sending..." else "Send")
                    }
                }
            }
        }
    }
}
