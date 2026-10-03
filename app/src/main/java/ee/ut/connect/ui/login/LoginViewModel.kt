package ee.ut.connect.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class LoginViewModel : ViewModel() {
    private var active = true
    var authenticated by mutableStateOf(false)
        private set
    var creatingAccount by mutableStateOf(false)
        private set
    fun toggleMode() { if (!busy) { creatingAccount = !creatingAccount; error = null } }
    fun updateEmail(value: String) { email = value; error = null }
    fun updatePassword(value: String) { password = value; error = null }
    fun updateDisplayName(value: String) { displayName = value; error = null }
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var displayName by mutableStateOf("")
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun submit(createAccount: Boolean) {
        if (busy || authenticated) return
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
            if (!active) return@addOnCompleteListener
            val user = if (result.isSuccessful) result.result?.user else null
            if (!result.isSuccessful || user == null) {
                busy = false
                error = result.exception?.localizedMessage ?: "Authentication failed. Please try again."
            } else {
                val profile = firestore.collection("users").document(user.uid)
                profile.get().addOnSuccessListener { existing ->
                    if (!active) return@addOnSuccessListener
                    if (existing.exists()) {
                        busy = false
                        password = ""
                        authenticated = true
                    } else {
                        // Accounts made before profiles existed receive a private, neutral fallback name.
                        val profileName = if (createAccount) name else "User ${user.uid.take(6)}"
                        profile.set(mapOf(
                            "displayName" to profileName,
                            "createdAt" to FieldValue.serverTimestamp(),
                        )).addOnSuccessListener profileSaved@ {
                            if (!active) return@profileSaved
                            busy = false
                            password = ""
                            authenticated = true
                        }.addOnFailureListener { exception ->
                            if (!active) return@addOnFailureListener
                            auth.signOut()
                            busy = false
                            error = exception.localizedMessage ?: "Could not save your profile. Try signing in again."
                        }
                    }
                }.addOnFailureListener { exception ->
                    if (!active) return@addOnFailureListener
                    auth.signOut()
                    busy = false
                    error = exception.localizedMessage ?: "Could not load your profile. Try again."
                }
            }
        }
    }

    override fun onCleared() {
        active = false
        password = ""
        super.onCleared()
    }
}
