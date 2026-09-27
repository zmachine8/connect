package ee.ut.connect.ui.users

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import ee.ut.connect.data.model.ConnectUser
import ee.ut.connect.data.repository.FirebaseUserRepository

sealed interface UsersUiState {
    data object Loading : UsersUiState
    data class Success(val users: List<ConnectUser>) : UsersUiState
    data class Error(val message: String) : UsersUiState
}

class UsersViewModel(private val repository: FirebaseUserRepository) : ViewModel() {
    private var listener: ListenerRegistration? = null
    private var observing = false
    private var generation = 0
    var uiState: UsersUiState by mutableStateOf(UsersUiState.Loading)
        private set

    init { loadUsers() }

    fun loadUsers() {
        stopObserving()
        observing = true
        val currentGeneration = generation
        uiState = UsersUiState.Loading
        listener = repository.observeUsers { result ->
            if (!observing || generation != currentGeneration) return@observeUsers
            uiState = result.fold(
                onSuccess = { UsersUiState.Success(it) },
                onFailure = { UsersUiState.Error(it.localizedMessage ?: "Could not load users") },
            )
        }
    }

    fun stopObserving() {
        observing = false
        generation++
        listener?.remove()
        listener = null
    }

    override fun onCleared() {
        stopObserving()
        super.onCleared()
    }
}
