package ee.ut.connect.ui.login

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.TextButton
import ee.ut.connect.ui.theme.CopperDivider
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import ee.ut.connect.ui.theme.EmberButton
import ee.ut.connect.ui.theme.fantasyFieldColors
import androidx.compose.foundation.shape.CutCornerShape
import ee.ut.connect.ui.theme.AdaptiveContent
import ee.ut.connect.ui.theme.FantasyBackdrop
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginRoute(onAuthenticated: () -> Unit) {
    val viewModel: LoginViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    androidx.compose.runtime.LaunchedEffect(viewModel.authenticated) {
        if (viewModel.authenticated) onAuthenticated()
    }

    LoginScreen(
        creatingAccount = viewModel.creatingAccount,
        onToggleMode = viewModel::toggleMode,
        email = viewModel.email,
        password = viewModel.password,
        displayName = viewModel.displayName,
        busy = viewModel.busy,
        error = viewModel.error,
        onEmailChanged = viewModel::updateEmail,
        onPasswordChanged = viewModel::updatePassword,
        onDisplayNameChanged = viewModel::updateDisplayName,
        onSignIn = { viewModel.submit(false) },
        onCreateAccount = { viewModel.submit(true) },
    )
}

@Composable
fun LoginScreen(
    creatingAccount: Boolean,
    onToggleMode: () -> Unit,
    email: String,
    password: String,
    displayName: String,
    busy: Boolean,
    error: String?,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onSignIn: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.activity.compose.BackHandler(enabled = creatingAccount && !busy) {
        onToggleMode()
    }
    FantasyBackdrop(login = true) {
        AdaptiveContent(modifier) { compact ->
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).widthIn(max = 480.dp).fillMaxWidth().fillMaxHeight().verticalScroll(rememberScrollState()).padding(if (compact) 16.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                if (creatingAccount) TextButton(onClick = onToggleMode, enabled = !busy) {
                    Text("← Back to sign in")
                }
                if (!compact && !creatingAccount) Spacer(Modifier.height(96.dp))
                Text("Connect", style = if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall)
                Text("PEOPLE · IDEAS · TOGETHER", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = if (compact) 12.dp else 24.dp))
                if (creatingAccount) OutlinedTextField(
                    colors = fantasyFieldColors(),
                    shape = CutCornerShape(6.dp),
                    value = displayName,
                    onValueChange = onDisplayNameChanged,
                    label = { Text("Display name") },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    colors = fantasyFieldColors(),
                    shape = CutCornerShape(6.dp),
                    value = email,
                    onValueChange = onEmailChanged,
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    colors = fantasyFieldColors(),
                    shape = CutCornerShape(6.dp),
                    value = password,
                    onValueChange = onPasswordChanged,
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
                }
                if (busy) CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
                EmberButton(onClick = { if (creatingAccount) onCreateAccount() else onSignIn() }, enabled = !busy, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Text(if (creatingAccount) "Create account" else "Sign in  →", style = MaterialTheme.typography.titleLarge)
                }
                CopperDivider(Modifier.padding(top = if (compact) 8.dp else 20.dp, bottom = 4.dp))
                TextButton(onClick = onToggleMode, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(if (creatingAccount) "Back to sign in" else "Create account")
                }
            }
        }
    }
}
