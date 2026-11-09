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
    var messages by remember { mutableStateOf<List<GroupMessage>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    DisposableEffect(groupId) {
        val listener = repository.observeMessages(groupId) { result ->
            result.onSuccess { messages = it; error = null }.onFailure { error = it.localizedMessage }
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
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), reverseLayout = false, verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = draft, onValueChange = { draft = it.take(2000) }, label = { Text("Message group") },
                        modifier = Modifier.weight(1f), maxLines = 4)
                    Button(onClick = { repository.sendMessage(groupId, draft) { result ->
                        result.onSuccess { draft = "" }.onFailure { error = it.localizedMessage }
                    } }, enabled = draft.isNotBlank()) { Text("Send") }
                }
            }
        }
    }
}
