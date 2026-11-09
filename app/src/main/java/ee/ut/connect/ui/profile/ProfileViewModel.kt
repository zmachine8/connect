package ee.ut.connect.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    private val firestore = FirebaseFirestore.getInstance()
    private var active = true
    private var dirty = savedState.get<Boolean>("dirty") ?: false
    private var usernameDirty = false
    private var requestsDirty = false
    var name by mutableStateOf(savedState.get<String>("name") ?: "")
        private set
    var username by mutableStateOf("")
        private set
    var allowRequests by mutableStateOf(true)
        private set
    var busy by mutableStateOf(false)
        private set
    var loading by mutableStateOf(true)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    private val listener = uid?.let { id ->
        firestore.collection("users").document(id).addSnapshotListener { snapshot, error ->
            if (active) {
                loading = false
                if (error != null) message = error.localizedMessage ?: "Could not load profile"
                else if (snapshot != null && !dirty && !busy) {
                    name = snapshot.getString("displayName").orEmpty()
                    if (!usernameDirty) username = snapshot.getString("username").orEmpty()
                    if (!requestsDirty) allowRequests = snapshot.getBoolean("allowRequests") != false
                    savedState["name"] = name
                }
            }
        }
    }
    init {
        if (uid == null) { loading = false; message = "Sign in to edit your profile" }
    }
    fun updateName(value: String) {
        name = value
        dirty = true
        savedState["name"] = value
        savedState["dirty"] = true
        message = null
    }
    fun updateUsername(value: String) { username = value.lowercase().filter { it in 'a'..'z' || it in '0'..'9' || it == '_' }.take(20); usernameDirty = true; message = null }
    fun toggleRequests(value: Boolean) { allowRequests = value; requestsDirty = true }
    fun save() {
        if (busy || loading) return
        val id = uid ?: return
        val trimmed = name.trim()
        val handle = username.trim().removePrefix("@").lowercase()
        if (trimmed.length !in 2..40 || trimmed.contains('\n')) {
            message = "Use a display name of 2 to 40 characters."
            return
        }
        if (handle.length !in 3..20 || !handle.matches(Regex("[a-z0-9_]+"))) {
            message = "Username must be 3–20 characters: letters, numbers, or underscore."
            return
        }
        busy = true
        message = null
        val profileRef = firestore.collection("users").document(id)
        firestore.runTransaction { transaction ->
            val profile = transaction.get(profileRef)
            val oldHandle = profile.getString("username")?.lowercase()
            val newHandleRef = firestore.collection("usernames").document(handle)
            val currentReservation = transaction.get(newHandleRef)
            require(!currentReservation.exists() || currentReservation.getString("uid") == id) { "That username is already taken." }
            if (oldHandle != handle || !currentReservation.exists()) {
                transaction.set(newHandleRef, mapOf("uid" to id))
                if (oldHandle != handle) oldHandle?.let { transaction.delete(firestore.collection("usernames").document(it)) }
            }
            transaction.update(profileRef, mapOf(
                "displayName" to trimmed,
                "username" to handle,
                "allowRequests" to allowRequests,
            ))
            null
        }
            .addOnSuccessListener {
                if (active) {
                    busy = false
                    name = trimmed
                    username = handle
                    usernameDirty = false
                    requestsDirty = false
                    dirty = false
                    savedState["name"] = name
                    savedState["dirty"] = false
                    message = "Saved"
                }
            }
            .addOnFailureListener {
                if (active) { busy = false; message = it.localizedMessage ?: "Could not save" }
            }
    }
    fun sendPasswordReset() {
        val email = FirebaseAuth.getInstance().currentUser?.email
        if (email.isNullOrBlank()) { message = "No email address is available for this account."; return }
        busy = true
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnSuccessListener { if (active) { busy = false; message = "Password reset email sent to $email" } }
            .addOnFailureListener { if (active) { busy = false; message = it.localizedMessage ?: "Could not send password reset email" } }
    }
    override fun onCleared() {
        active = false
        listener?.remove()
        super.onCleared()
    }
}
