package ee.ut.connect.ui.users

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ee.ut.connect.data.model.ConnectUser
import ee.ut.connect.data.repository.FakeUserRepository

@Composable
fun UsersRoute(
    onUserSelected: (ConnectUser) -> Unit,
    viewModel: UsersViewModel = viewModel { UsersViewModel(FakeUserRepository()) },
) {
    UsersScreen(viewModel.uiState, onUserSelected, viewModel::loadUsers)
}

@Composable
fun UsersScreen(
    state: UsersUiState,
    onUserSelected: (ConnectUser) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("People", style = MaterialTheme.typography.headlineMedium)
        Text("Choose someone to open a prototype chat.", modifier = Modifier.padding(bottom = 16.dp))
        when (state) {
            UsersUiState.Loading -> CircularProgressIndicator()
            is UsersUiState.Error -> {
                Text(state.message)
                Button(onClick = onRetry) { Text("Try again") }
            }
            is UsersUiState.Success -> LazyColumn {
                items(state.users, key = { it.id }) { user ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onUserSelected(user) }.padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(user.displayName)
                        Text(if (user.isOnline) "Online" else "Offline", style = MaterialTheme.typography.labelMedium)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
