package ee.ut.connect.data.repository

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
        val states = mutableMapOf<String, Boolean>()
        val registrations = mutableListOf<Pair<com.google.firebase.database.DatabaseReference, ValueEventListener>>()
        if (ids.isEmpty()) onChange(emptyMap())

        ids.forEach { uid ->
            val reference = activeDatabase.reference.child("status").child(uid)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    states[uid] = snapshot.children.any { child ->
                        child.child("state").getValue(String::class.java) == "online"
                    }
                    onChange(states.toMap())
                }

                override fun onCancelled(error: DatabaseError) {
                    states.remove(uid)
                    onChange(states.toMap())
                }
            }
            registrations += reference to listener
            reference.addValueEventListener(listener)
        }
        return PresenceSubscription {
            registrations.forEach { (reference, listener) -> reference.removeEventListener(listener) }
        }
    }
}
