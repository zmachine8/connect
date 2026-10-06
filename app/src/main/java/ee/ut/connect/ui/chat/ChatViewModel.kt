package ee.ut.connect.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import com.google.firebase.firestore.ListenerRegistration
import ee.ut.connect.data.model.ChatMessage
import ee.ut.connect.data.repository.FirestoreChatRepository

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val loading: Boolean = true,
    val sending: Boolean = false,
    val uploading: Boolean = false,
    val error: String? = null,
)

class ChatViewModel(otherId: String, private val repository: FirestoreChatRepository, private val savedState: SavedStateHandle) : ViewModel() {
    var uiState by mutableStateOf(ChatUiState(draft = savedState.get<String>("draft") ?: ""))
        private set
    private var chatId: String? = null
    private var listener: ListenerRegistration? = null
    private var active = true

    init {
        repository.open(otherId) { result ->
            if (!active) return@open
            result.fold(onSuccess = { id ->
                chatId = id
                listener = repository.observe(id) { messages ->
                    if (!active) return@observe
                    uiState = messages.fold(
                        onSuccess = { uiState.copy(messages = it, loading = false) },
                        onFailure = { uiState.copy(loading = false, error = it.localizedMessage) },
                    )
                }
            }, onFailure = {
                uiState = uiState.copy(loading = false, error = it.localizedMessage)
            })
        }
    }

    fun updateDraft(value: String) { uiState = uiState.copy(draft = value); savedState["draft"] = value }

    fun send() {
        val id = chatId ?: return
        val text = uiState.draft.trim()
        if (text.isEmpty() || uiState.sending || uiState.uploading) return
        uiState = uiState.copy(sending = true, error = null)
        repository.send(id, text) { result ->
            if (!active) return@send
            uiState = if (result.isSuccess) uiState.copy(
                draft = if (uiState.draft.trim() == text) "" else uiState.draft,
                sending = false)
            else uiState.copy(sending = false, error = result.exceptionOrNull()?.localizedMessage)
            savedState["draft"] = uiState.draft
        }
    }

    fun sendPhoto(jpeg: ByteArray, onComplete: (Boolean) -> Unit) {
        val id = chatId ?: return
        if (uiState.uploading || uiState.sending) return
        val caption = uiState.draft.trim()
        uiState = uiState.copy(uploading = true, error = null)
        repository.sendPhoto(id, jpeg, caption) { result ->
            if (!active) return@sendPhoto
            uiState = uiState.copy(uploading = false,
                draft = if (result.isSuccess && uiState.draft.trim() == caption) "" else uiState.draft,
                error = result.exceptionOrNull()?.localizedMessage)
            savedState["draft"] = uiState.draft
            onComplete(result.isSuccess)
        }
    }

    fun sendDocument(name: String, mimeType: String, bytes: ByteArray, onComplete: (Boolean) -> Unit) {
        val id = chatId ?: return
        if (uiState.uploading || uiState.sending) return
        val caption = uiState.draft.trim()
        uiState = uiState.copy(uploading = true, error = null)
        repository.sendDocument(id, name, mimeType, bytes, caption) { result ->
            if (!active) return@sendDocument
            uiState = uiState.copy(uploading = false,
                draft = if (result.isSuccess && uiState.draft.trim() == caption) "" else uiState.draft,
                error = result.exceptionOrNull()?.localizedMessage)
            savedState["draft"] = uiState.draft
            onComplete(result.isSuccess)
        }
    }

    fun sendLocation(latitude: Double, longitude: Double) {
        val id = chatId ?: return
        if (uiState.uploading || uiState.sending) return
        uiState = uiState.copy(sending = true, error = null)
        repository.sendLocation(id, latitude, longitude) { result ->
            if (!active) return@sendLocation
            uiState = uiState.copy(sending = false, error = result.exceptionOrNull()?.localizedMessage)
        }
    }

    fun showError(message: String) { uiState = uiState.copy(error = message) }


    override fun onCleared() {
        active = false
        listener?.remove()
        super.onCleared()
    }
}
