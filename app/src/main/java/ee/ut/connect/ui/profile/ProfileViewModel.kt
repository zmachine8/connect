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
    var name by mutableStateOf(savedState.get<String>("name") ?: "")
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
    fun save() {
        if (busy || loading) return
        val id = uid ?: return
        val trimmed = name.trim()
        if (trimmed.length !in 2..40 || trimmed.contains('\n')) {
            message = "Use a display name of 2 to 40 characters."
            return
        }
        busy = true
        message = null
        firestore.collection("users").document(id).update("displayName", trimmed)
            .addOnSuccessListener {
                if (active) {
                    busy = false
                    name = trimmed
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
    override fun onCleared() {
        active = false
        listener?.remove()
        super.onCleared()
    }
}
