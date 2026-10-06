package ee.ut.connect.ui.users

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.shape.CutCornerShape
import ee.ut.connect.ui.theme.CopperDivider
import ee.ut.connect.ui.theme.AdaptiveContent
import ee.ut.connect.ui.theme.FantasyBackdrop
import ee.ut.connect.ui.theme.InitialAvatar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ee.ut.connect.data.model.ConnectUser
import ee.ut.connect.data.repository.FirebaseUserRepository

@Composable
fun UsersRoute(
    onUserSelected: (ConnectUser) -> Unit,
    onSignOut: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: UsersViewModel = viewModel { UsersViewModel(FirebaseUserRepository()) },
) {
    UsersScreen(viewModel.uiState, onUserSelected, viewModel::loadUsers, onOpenProfile, onSignOut = {
        viewModel.stopObserving()
        onSignOut()
    })
}

@Composable
fun UsersScreen(
    state: UsersUiState,
    onUserSelected: (ConnectUser) -> Unit,
    onRetry: () -> Unit,
    onOpenProfile: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FantasyBackdrop {
        AdaptiveContent(modifier) { compact ->
            Column(Modifier.align(Alignment.TopCenter).widthIn(max = 840.dp).fillMaxSize().padding(if (compact) 8.dp else 20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("People", style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenProfile) { Text("Profile") }
                    TextButton(onClick = onSignOut) { Text("Sign out") }
                }
                CopperDivider(Modifier.padding(top = if (compact) 0.dp else 8.dp, bottom = if (compact) 4.dp else 20.dp))
                when (state) {
                    UsersUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is UsersUiState.Error -> {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onRetry) { Text("Try again") }
                    }
                    is UsersUiState.Success -> if (state.users.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No other registered people yet")
                        }
                    } else LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)) {
                        items(state.users, key = { it.id }) { user ->
                            Surface(shape = CutCornerShape(8.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .94f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .6f))) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { onUserSelected(user) }.padding(if (compact) 10.dp else 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                InitialAvatar(user.displayName, Modifier.size(if (compact) 40.dp else 52.dp))
                                Text(user.displayName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Text("›", color = MaterialTheme.colorScheme.primary)
                            }
                            }
                        }
                    }
                }
            }
        }
    }
}
