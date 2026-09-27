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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoginRoute(onAuthenticated: () -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit(createAccount: Boolean) {
        val address = email.trim()
        val name = displayName.trim()
        if (address.isBlank() || password.isBlank()) {
            error = "Enter an email and password."
            return
        }
        if (createAccount && (name.length !in 2..40 || name.contains('\n'))) {
            error = "Enter a display name of 2 to 40 characters."
            return
        }
        busy = true
        error = null
        val task = if (createAccount) auth.createUserWithEmailAndPassword(address, password)
        else auth.signInWithEmailAndPassword(address, password)
        task.addOnCompleteListener { result ->
            val user = if (result.isSuccessful) result.result?.user else null
            if (!result.isSuccessful || user == null) {
                busy = false
                error = result.exception?.localizedMessage ?: "Authentication failed. Please try again."
            } else {
                val profile = firestore.collection("users").document(user.uid)
                profile.get().addOnSuccessListener { existing ->
                    if (existing.exists()) {
                        busy = false
                        onAuthenticated()
                    } else {
                        // Accounts made before profiles existed receive a private, neutral fallback name.
                        val profileName = if (createAccount) name else "User ${user.uid.take(6)}"
                        profile.set(mapOf(
                            "displayName" to profileName,
                            "createdAt" to FieldValue.serverTimestamp(),
                        )).addOnSuccessListener {
                            busy = false
                            onAuthenticated()
                        }.addOnFailureListener { exception ->
                            auth.signOut()
                            busy = false
                            error = exception.localizedMessage ?: "Could not save your profile. Try signing in again."
                        }
                    }
                }.addOnFailureListener { exception ->
                    auth.signOut()
                    busy = false
                    error = exception.localizedMessage ?: "Could not load your profile. Try again."
                }
            }
        }
    }

    LoginScreen(
        email = email,
        password = password,
        displayName = displayName,
        busy = busy,
        error = error,
        onEmailChanged = { email = it; error = null },
        onPasswordChanged = { password = it; error = null },
        onDisplayNameChanged = { displayName = it; error = null },
        onSignIn = { submit(false) },
        onCreateAccount = { submit(true) },
    )
}

@Composable
fun LoginScreen(
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
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Connect", style = MaterialTheme.typography.displaySmall)
        Text("Private conversations with people you know", modifier = Modifier.padding(vertical = 16.dp))
        OutlinedTextField(
            value = displayName,
            onValueChange = onDisplayNameChanged,
            label = { Text("Display name (new accounts)") },
            singleLine = true,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
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
