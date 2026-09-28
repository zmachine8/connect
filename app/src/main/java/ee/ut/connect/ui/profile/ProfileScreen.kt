package ee.ut.connect.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileRoute(onBack: () -> Unit) {
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid }
    val firestore = remember { FirebaseFirestore.getInstance() }
    var name by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    DisposableEffect(uid) {
        val registration = uid?.let { id ->
            firestore.collection("users").document(id).addSnapshotListener { snapshot, error ->
                if (error != null) message = error.localizedMessage
                else if (snapshot != null && !busy) name = snapshot.getString("displayName").orEmpty()
            }
        }
        onDispose { registration?.remove() }
    }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Text("Your profile")
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; message = null },
            label = { Text("Display name") },
            enabled = !busy,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = {
            val trimmed = name.trim()
            if (uid == null || trimmed.length !in 2..40 || trimmed.contains('\n')) {
                message = "Use a display name of 2 to 40 characters."
            } else {
                busy = true
                firestore.collection("users").document(uid).update("displayName", trimmed)
                    .addOnSuccessListener { busy = false; name = trimmed; message = "Saved" }
                    .addOnFailureListener { busy = false; message = it.localizedMessage ?: "Could not save" }
            }
        }, enabled = !busy) { Text("Save") }
        if (message != null) Text(message.orEmpty())
    }
}
