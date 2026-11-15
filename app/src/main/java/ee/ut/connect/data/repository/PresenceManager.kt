package ee.ut.connect.data.repository

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

/** Publishes this app instance's foreground presence and cleans it up on disconnect. */
class PresenceManager(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase? = runCatching { FirebaseDatabase.getInstance() }.getOrNull(),
) : DefaultLifecycleObserver, AutoCloseable {
    private var uid: String? = auth.currentUser?.uid
    private var foreground = false
    private var connected = false
    private var presenceRef: DatabaseReference? = null

    private val connectedRef = database?.getReference(".info/connected")
    private val connectionListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            connected = snapshot.getValue(Boolean::class.java) == true
            if (connected) publishOnline() else publishOffline()
        }

        override fun onCancelled(error: DatabaseError) {
            connected = false
            publishOffline()
        }
    }

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val newUid = firebaseAuth.currentUser?.uid
        if (newUid != uid) {
            publishOffline()
            presenceRef = null
            uid = newUid
            if (connected) publishOnline()
        }
    }

    init {
        auth.addAuthStateListener(authListener)
        connectedRef?.addValueEventListener(connectionListener)
    }

    override fun onStart(owner: LifecycleOwner) {
        foreground = true
        if (connected) publishOnline()
    }

    override fun onStop(owner: LifecycleOwner) {
        foreground = false
        publishOffline()
    }

    private fun publishOnline() {
        val currentUid = uid ?: return
        if (!foreground || !connected) return
        val ref = presenceRef ?: database?.getReference("status")?.child(currentUid)?.push()?.also {
            presenceRef = it
        } ?: return

        ref.onDisconnect().setValue(presenceRecord("offline"))
            .addOnSuccessListener {
                if (uid == currentUid && foreground && connected && presenceRef == ref) {
                    ref.setValue(presenceRecord("online"))
                }
            }
    }

    private fun publishOffline() {
        presenceRef?.setValue(presenceRecord("offline"))
    }

    private fun presenceRecord(state: String) = mapOf(
        "state" to state,
        "last_changed" to ServerValue.TIMESTAMP,
    )

    override fun close() {
        foreground = false
        publishOffline()
        auth.removeAuthStateListener(authListener)
        connectedRef?.removeEventListener(connectionListener)
    }
}
