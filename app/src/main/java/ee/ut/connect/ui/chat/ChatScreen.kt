package ee.ut.connect.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ee.ut.connect.data.repository.FakeChatRepository

@Composable
fun ChatRoute(userId: String, displayName: String, onBack: () -> Unit) {
    val viewModel: ChatViewModel = viewModel(key = userId) { ChatViewModel(userId, FakeChatRepository()) }
    ChatScreen(displayName, viewModel.uiState, onBack, viewModel::updateDraft, viewModel::send)
}

@Composable
fun ChatScreen(
    displayName: String,
    state: ChatUiState,
    onBack: () -> Unit,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Text(displayName, modifier = Modifier.padding(top = 12.dp))
        }
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp)) {
            items(state.messages, key = { it.id }) { message ->
                Text(message.text, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
        OutlinedTextField(
            value = state.draft,
            onValueChange = onDraftChanged,
            label = { Text("Message") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = onSend, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Send locally") }
    }
}
