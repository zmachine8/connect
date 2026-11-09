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
    fun updateUsername(value: String) { username = value.lowercase().filter { it in 'a'..'z' || it in '0'..'9' || it == '_' }.take(20); error = null }
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var displayName by mutableStateOf("")
        private set
    var username by mutableStateOf("")
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun submit(createAccount: Boolean) {
        if (busy || authenticated) return
        val address = email.trim()
        val name = displayName.trim()
        val handle = username.trim().removePrefix("@").lowercase()
        if (address.isBlank() || password.isBlank()) {
            error = "Enter an email and password."
            return
        }
        if (createAccount && (name.length !in 2..40 || name.contains('\n'))) {
            error = "Enter a display name of 2 to 40 characters."
            return
        }
        if (createAccount && (handle.length !in 3..20 || !handle.matches(Regex("[a-z0-9_]+")))) {
            error = "Choose a username with 3–20 letters, numbers, or underscores."
            return
        }
        busy = true
        error = null
        if (createAccount) {
            firestore.collection("usernames").document(handle).get().addOnSuccessListener { reservation ->
                if (!active) return@addOnSuccessListener
                if (reservation.exists()) {
                    busy = false
                    error = "That username is already taken. Choose another one."
                } else authenticate(address, name, handle, createAccount)
            }.addOnFailureListener { exception ->
                if (active) { busy = false; error = exception.localizedMessage ?: "Could not check username availability." }
            }
            return
        }
        authenticate(address, name, handle, createAccount)
    }

    private fun authenticate(address: String, name: String, handle: String, createAccount: Boolean) {
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
                        val profileData = mutableMapOf<String, Any>(
                            "displayName" to profileName,
                            "createdAt" to FieldValue.serverTimestamp(),
                        )
                        if (createAccount) {
                            profileData["username"] = handle
                            profileData["allowRequests"] = true
                        }
                        val saveTask = if (createAccount) {
                            val usernameRef = firestore.collection("usernames").document(handle)
                            firestore.runTransaction { transaction ->
                                val reservation = transaction.get(usernameRef)
                                require(!reservation.exists()) { "That username was just claimed. Sign in and choose another in Profile." }
                                transaction.set(usernameRef, mapOf("uid" to user.uid))
                                transaction.set(profile, profileData)
                                null
                            }
                        } else profile.set(profileData)
                        saveTask.addOnSuccessListener profileSaved@ {
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
