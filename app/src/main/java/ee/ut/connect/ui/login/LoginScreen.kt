package ee.ut.connect.ui.login

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginRoute(onAuthenticated: () -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit(createAccount: Boolean) {
        val address = email.trim()
        if (address.isBlank() || password.isBlank()) {
            error = "Enter an email and password."
            return
        }
        busy = true
        error = null
        val task = if (createAccount) auth.createUserWithEmailAndPassword(address, password)
        else auth.signInWithEmailAndPassword(address, password)
        task.addOnCompleteListener { result ->
            busy = false
            if (result.isSuccessful) onAuthenticated()
            else error = result.exception?.localizedMessage ?: "Authentication failed. Please try again."
        }
    }

    LoginScreen(
        email = email,
        password = password,
        busy = busy,
        error = error,
        onEmailChanged = { email = it; error = null },
        onPasswordChanged = { password = it; error = null },
        onSignIn = { submit(false) },
        onCreateAccount = { submit(true) },
    )
}

@Composable
fun LoginScreen(
    email: String,
    password: String,
    busy: Boolean,
    error: String?,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSignIn: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Connect", style = MaterialTheme.typography.displaySmall)
        Text("Private conversations with people you know", modifier = Modifier.padding(vertical = 16.dp))
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChanged,
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
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
        Button(onClick = onSignIn, enabled = !busy, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text("Sign in")
        }
        OutlinedButton(onClick = onCreateAccount, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text("Create account")
        }
    }
}
