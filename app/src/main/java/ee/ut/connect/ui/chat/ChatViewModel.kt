package ee.ut.connect.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ee.ut.connect.data.model.ChatMessage
import ee.ut.connect.data.repository.ChatRepository
import kotlinx.coroutines.launch

data class ChatUiState(val messages: List<ChatMessage> = emptyList(), val draft: String = "")

class ChatViewModel(private val userId: String, private val repository: ChatRepository) : ViewModel() {
    var uiState by mutableStateOf(ChatUiState())
        private set

    init { viewModelScope.launch { uiState = uiState.copy(messages = repository.getMessages(userId)) } }

    fun updateDraft(value: String) { uiState = uiState.copy(draft = value) }

    fun send() {
        val text = uiState.draft.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            repository.sendMessage(userId, text)
            val localMessage = ChatMessage("local-${uiState.messages.size}", "me", text)
            uiState = uiState.copy(messages = uiState.messages + localMessage, draft = "")
        }
    }
}
