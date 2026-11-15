package ee.ut.connect.data.repository

import android.os.Handler
import android.os.Looper
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

fun interface PresenceSubscription {
    fun remove()
}

class PresenceRepository(
    private val database: FirebaseDatabase? = runCatching { FirebaseDatabase.getInstance() }.getOrNull(),
) {
    fun observe(userIds: Collection<String>, onChange: (Map<String, Boolean>) -> Unit): PresenceSubscription {
        val activeDatabase = database ?: run {
            onChange(emptyMap())
            return PresenceSubscription { }
        }
        val ids = userIds.distinct().filter { it.isNotBlank() }
        val handler = Handler(Looper.getMainLooper())
        val sessionsByUser = mutableMapOf<String, List<PresenceSession>>()
        val registrations = mutableListOf<Pair<com.google.firebase.database.DatabaseReference, ValueEventListener>>()
        var active = true

        fun publish() {
            if (!active) return
            val now = System.currentTimeMillis()
            onChange(ids.associateWith { uid ->
                sessionsByUser[uid].orEmpty().any { session ->
                    session.state == "online" && session.lastChangedMillis != null &&
                        now - session.lastChangedMillis <= STALE_AFTER_MS
                }
            })
        }

        val refresh = object : Runnable {
            override fun run() {
                publish()
                if (active) handler.postDelayed(this, REFRESH_INTERVAL_MS)
            }
        }
        if (ids.isEmpty()) onChange(emptyMap())

        ids.forEach { uid ->
            val reference = activeDatabase.reference.child("status").child(uid)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    sessionsByUser[uid] = snapshot.children.map { child ->
                        PresenceSession(
                            state = child.child("state").getValue(String::class.java),
                            lastChangedMillis = child.child("last_changed").getValue(Long::class.java),
                        )
                    }
                    publish()
                }

                override fun onCancelled(error: DatabaseError) {
                    sessionsByUser.remove(uid)
                    publish()
                }
            }
            registrations += reference to listener
            reference.addValueEventListener(listener)
        }
        handler.postDelayed(refresh, REFRESH_INTERVAL_MS)

        return PresenceSubscription {
            active = false
            handler.removeCallbacks(refresh)
            registrations.forEach { (reference, listener) -> reference.removeEventListener(listener) }
        }
    }

    private data class PresenceSession(val state: String?, val lastChangedMillis: Long?)

    private companion object {
        // The manager refreshes active records every 30 seconds. This leaves room for a
        // missed update while ensuring abandoned sessions stop appearing online quickly.
        const val STALE_AFTER_MS = 90_000L
        const val REFRESH_INTERVAL_MS = 30_000L
    }
}
