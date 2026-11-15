package ee.ut.connect.data.repository

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Publishes foreground presence and refreshes its timestamp so stale sessions can expire. */
class PresenceManager(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase? = runCatching { FirebaseDatabase.getInstance() }.getOrNull(),
) : DefaultLifecycleObserver, AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var uid: String? = auth.currentUser?.uid
    private var foreground = false
    private var connected = false
    private var presenceRef: DatabaseReference? = null
    private var registrationPending = false
    private var heartbeatJob: Job? = null

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
        if (!foreground || !connected || heartbeatJob?.isActive == true || registrationPending) return
        val ref = presenceRef ?: database?.getReference("status")?.child(currentUid)?.push()?.also {
            presenceRef = it
        } ?: return

        registrationPending = true
        ref.onDisconnect().setValue(presenceRecord("offline"))
            .addOnCompleteListener { task ->
                registrationPending = false
                if (task.isSuccessful) {
                    startHeartbeat(ref, currentUid)
                } else if (uid == currentUid && foreground && connected && presenceRef == ref) {
                    // Keep the UI current; the timestamp timeout is a fallback if server cleanup fails.
                    startHeartbeat(ref, currentUid)
                }
            }
    }

    private fun startHeartbeat(ref: DatabaseReference, currentUid: String) {
        if (uid != currentUid || !foreground || !connected || presenceRef != ref || heartbeatJob?.isActive == true) return
        ref.setValue(presenceRecord("online"))
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                if (uid != currentUid || !foreground || !connected || presenceRef != ref) break
                ref.setValue(presenceRecord("online"))
            }
        }
    }

    private fun publishOffline() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        presenceRef?.setValue(presenceRecord("offline"))
    }

    private fun presenceRecord(state: String) = mapOf(
        "state" to state,
        // Refreshed by the heartbeat; readers use it to expire stale online records.
        "last_changed" to ServerValue.TIMESTAMP,
    )

    override fun close() {
        foreground = false
        publishOffline()
        auth.removeAuthStateListener(authListener)
        connectedRef?.removeEventListener(connectionListener)
        scope.cancel()
    }

    private companion object {
        const val HEARTBEAT_INTERVAL_MS = 30_000L
    }
}
