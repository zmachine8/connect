package ee.ut.connect.ui.profile

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.MaterialTheme
import ee.ut.connect.ui.theme.EmberButton
import ee.ut.connect.ui.theme.fantasyFieldColors
import androidx.compose.foundation.shape.CutCornerShape
import ee.ut.connect.ui.theme.AdaptiveContent
import ee.ut.connect.ui.theme.FantasyBackdrop
import ee.ut.connect.ui.theme.InitialAvatar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ee.ut.connect.ui.theme.ConnectThemeStyle
import ee.ut.connect.ui.theme.LocalConnectThemeStyle

@Composable
fun ProfileRoute(onBack: () -> Unit, onThemeChange: (ConnectThemeStyle) -> Unit = {}) {
    val viewModel: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val themeStyle = LocalConnectThemeStyle.current
    var themeMenuExpanded by remember { mutableStateOf(false) }
    FantasyBackdrop {
        AdaptiveContent { compact ->
            Column(Modifier.align(Alignment.TopCenter).widthIn(max = 560.dp).fillMaxWidth().fillMaxHeight().verticalScroll(rememberScrollState()).padding(if (compact) 12.dp else 24.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onBack) { Text("Back") }
                    Text("Your profile", style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (compact) InitialAvatar(viewModel.name, Modifier.size(40.dp))
                }
                if (!compact) InitialAvatar(viewModel.name)
                OutlinedTextField(
                    colors = fantasyFieldColors(),
                    shape = CutCornerShape(6.dp),
                    value = viewModel.name,
                    onValueChange = viewModel::updateName,
                    label = { Text("Display name") },
                    enabled = !viewModel.busy && !viewModel.loading,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    colors = fantasyFieldColors(), shape = CutCornerShape(6.dp),
                    value = viewModel.username, onValueChange = viewModel::updateUsername,
                    label = { Text("Username") },
                    supportingText = { Text("People find you by this unique username. Letters, numbers and _ only.") },
                    enabled = !viewModel.busy && !viewModel.loading, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Privacy", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Allow connection requests", modifier = Modifier.weight(1f))
                    Switch(checked = viewModel.allowRequests, onCheckedChange = viewModel::toggleRequests,
                        enabled = !viewModel.busy && !viewModel.loading)
                }
                EmberButton(onClick = viewModel::save, enabled = !viewModel.busy && !viewModel.loading) { Text("Save profile and privacy") }
                Text("Account", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = viewModel::sendPasswordReset, enabled = !viewModel.busy) {
                    Text("Send password reset email")
                }
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                androidx.compose.foundation.layout.Box {
                    TextButton(onClick = { themeMenuExpanded = true }) {
                        Text("Theme: ${themeStyle.label}")
                    }
                    DropdownMenu(expanded = themeMenuExpanded, onDismissRequest = { themeMenuExpanded = false }) {
                        ConnectThemeStyle.entries.forEach { style ->
                            DropdownMenuItem(
                                text = { Text(if (style == themeStyle) "${style.label} ✓" else style.label) },
                                onClick = { themeMenuExpanded = false; onThemeChange(style) },
                            )
                        }
                    }
                }
                if (viewModel.loading) Text("Loading profile…")
                if (viewModel.message != null) Text(viewModel.message.orEmpty())
            }
        }
    }
}
