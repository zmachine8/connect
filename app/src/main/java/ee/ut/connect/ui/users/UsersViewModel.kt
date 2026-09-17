package ee.ut.connect.ui.users

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ee.ut.connect.data.model.ConnectUser
import ee.ut.connect.data.repository.UserRepository
import kotlinx.coroutines.launch

sealed interface UsersUiState {
    data object Loading : UsersUiState
    data class Success(val users: List<ConnectUser>) : UsersUiState
    data class Error(val message: String) : UsersUiState
}

class UsersViewModel(private val repository: UserRepository) : ViewModel() {
    var uiState: UsersUiState by mutableStateOf(UsersUiState.Loading)
        private set

    init { loadUsers() }

    fun loadUsers() {
        uiState = UsersUiState.Loading
        viewModelScope.launch {
            uiState = runCatching { repository.getUsers() }
                .fold(
                    onSuccess = { UsersUiState.Success(it) },
                    onFailure = { UsersUiState.Error(it.message ?: "Could not load users") },
                )
        }
    }
}
