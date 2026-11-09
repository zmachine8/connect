package ee.ut.connect.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import ee.ut.connect.data.model.ConnectUser

data class ContactRequest(val id: String, val senderId: String, val senderName: String)

data class PeopleSnapshot(
    val myDisplayName: String,
    val users: List<ConnectUser>,
    val blockedUsers: List<ConnectUser>,
    val connectionIds: Set<String>,
    val incomingRequests: List<ContactRequest>,
    val outgoingRequestIds: Set<String>,
    val blockedIds: Set<String>,
    val hiddenUserIds: Set<String> = emptySet(),
)

class PeopleRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun searchByUsername(value: String, onResult: (Result<List<ConnectUser>>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onResult(Result.failure(IllegalStateException("Sign in first")))
        val prefix = value.trim().removePrefix("@").lowercase()
        if (!prefix.matches(Regex("[a-z0-9_]{2,20}"))) return onResult(Result.success(emptyList()))
        firestore.collection("usernames").orderBy(FieldPath.documentId())
            .startAt(prefix).endAt(prefix + "\uf8ff").limit(50).get()
            .addOnSuccessListener { reservations ->
                val entries = reservations.documents.mapNotNull { doc ->
                    val owner = doc.getString("uid") ?: return@mapNotNull null
                    if (owner == uid) null else owner to doc.id
                }
                if (entries.isEmpty()) return@addOnSuccessListener onResult(Result.success(emptyList()))
                val users = mutableListOf<ConnectUser>()
                var remaining = entries.size
                entries.forEach { (owner, username) ->
                    firestore.collection("users").document(owner).get()
                        .addOnSuccessListener { profile ->
                            val displayName = profile.getString("displayName")
                            if (profile.exists() && !displayName.isNullOrBlank()) {
                                users += ConnectUser(profile.id, displayName, false, username)
                            }
                            remaining--
                            if (remaining == 0) onResult(Result.success(users))
                        }.addOnFailureListener { failure ->
                            remaining--
                            if (remaining == 0) onResult(Result.failure(failure))
                        }
                }
            }.addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun observePeople(onResult: (Result<PeopleSnapshot>) -> Unit): ListenerRegistration {
        val uid = auth.currentUser?.uid ?: run {
            onResult(Result.failure(IllegalStateException("Sign in to see people")))
            return EmptyListener
        }
        var myDisplayName = "Connect user"
        var connections: Set<String>? = null
        var legacyChats: Set<String>? = null
        var incoming: List<ContactRequest>? = null
        var outgoing: Set<String>? = null
        var blocked: Set<String>? = null
        var blockingMe: Set<String>? = null
        var failed: Throwable? = null
        var active = true
        fun publish() {
            if (!active || auth.currentUser?.uid != uid) return
            failed?.let { onResult(Result.failure(it)); return }
            val c = connections ?: return
            val l = legacyChats ?: return
            val i = incoming ?: return
            val o = outgoing ?: return
            val b = blocked ?: return
            val bm = blockingMe ?: return
            val visibleIds = ((c + l) + b + i.map { it.senderId }).filter { it != uid && it !in bm }.toSet()
            val snapshot = PeopleSnapshot(myDisplayName, emptyList(), emptyList(), c + l,
                i.filter { it.senderId !in b && it.senderId !in bm }, o, b, b + bm)
            if (visibleIds.isEmpty()) { onResult(Result.success(snapshot)); return }
            val results = mutableListOf<ConnectUser>()
            var remaining = visibleIds.size
            visibleIds.forEach { personId ->
                firestore.collection("users").document(personId).get()
                    .addOnSuccessListener { doc ->
                        val displayName = doc.getString("displayName")
                        if (doc.exists() && !displayName.isNullOrBlank()) results += ConnectUser(
                            doc.id, displayName, false, doc.getString("username").orEmpty())
                        remaining--
                        if (remaining == 0 && active) {
                            val ordered = results.sortedBy { it.displayName.lowercase() }
                            onResult(Result.success(snapshot.copy(
                                users = ordered.filter { it.id !in b && it.id !in bm },
                                blockedUsers = ordered.filter { it.id in b },
                            )))
                        }
                    }
                    .addOnFailureListener { failure ->
                        remaining--
                        if (remaining == 0 && active) onResult(Result.failure(failure))
                    }
            }
        }
        val regs = mutableListOf<ListenerRegistration>()
        firestore.collection("users").document(uid).get().addOnSuccessListener { doc ->
            myDisplayName = doc.getString("displayName") ?: "Connect user"
            publish()
        }.addOnFailureListener { failed = it; publish() }
        regs += firestore.collection("connections").whereArrayContains("participantIds", uid).addSnapshotListener { snap, error ->
            if (error != null) failed = error else if (snap != null) {
                connections = snap.documents.flatMap { it.get("participantIds") as? List<*> ?: emptyList<Any>() }
                    .filterIsInstance<String>().filter { it != uid }.toSet()
            }
            publish()
        }
        regs += firestore.collection("chats").whereArrayContains("participantIds", uid).addSnapshotListener { snap, error ->
            if (error != null) failed = error else if (snap != null) {
                legacyChats = snap.documents.flatMap { it.get("participantIds") as? List<*> ?: emptyList<Any>() }
                    .filterIsInstance<String>().filter { it != uid }.toSet()
            }
            publish()
        }
        regs += firestore.collection("contactRequests").whereEqualTo("recipientId", uid)
            .addSnapshotListener { snap, error ->
                if (error != null) failed = error else if (snap != null) {
                    incoming = snap.documents.mapNotNull { doc ->
                        if (doc.getString("status") != "pending") return@mapNotNull null
                        val sender = doc.getString("senderId") ?: return@mapNotNull null
                        val name = doc.getString("senderName") ?: "Connect user"
                        ContactRequest(doc.id, sender, name)
                    }
                }
                publish()
            }
        regs += firestore.collection("contactRequests").whereEqualTo("senderId", uid)
            .addSnapshotListener { snap, error ->
                if (error != null) failed = error else if (snap != null) {
                    outgoing = snap.documents.filter { it.getString("status") == "pending" }
                        .mapNotNull { it.getString("recipientId") }.toSet()
                }
                publish()
            }
        regs += firestore.collection("blocks").whereEqualTo("blockerId", uid).addSnapshotListener { snap, error ->
            if (error != null) failed = error else if (snap != null) blocked = snap.documents.mapNotNull { it.getString("blockedUserId") }.toSet()
            publish()
        }
        regs += firestore.collection("blocks").whereEqualTo("blockedUserId", uid).addSnapshotListener { snap, error ->
            if (error != null) failed = error else if (snap != null) blockingMe = snap.documents.mapNotNull { it.getString("blockerId") }.toSet()
            publish()
        }
        return object : ListenerRegistration {
            override fun remove() { active = false; regs.forEach { it.remove() } }
        }
    }

    fun sendRequest(user: ConnectUser, onComplete: (Result<Unit>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onComplete(Result.failure(IllegalStateException("Sign in first")))
        if (user.id == uid) return onComplete(Result.failure(IllegalArgumentException("You cannot add yourself")))
        firestore.collection("users").document(uid).get().addOnSuccessListener { profile ->
            val name = profile.getString("displayName") ?: "Connect user"
            val requestRef = firestore.collection("contactRequests").document("${uid}_${user.id}")
            requestRef.get().addOnSuccessListener { existing ->
                when {
                    existing.getString("status") == "pending" -> onComplete(Result.success(Unit))
                    existing.getString("status") == "declined" -> requestRef.update("status", "pending")
                        .addOnSuccessListener { onComplete(Result.success(Unit)) }
                        .addOnFailureListener { onComplete(Result.failure(it)) }
                    existing.exists() -> onComplete(Result.success(Unit))
                    else -> {
                        val data = mapOf(
                            "senderId" to uid, "senderName" to name, "recipientId" to user.id,
                            "status" to "pending", "createdAt" to FieldValue.serverTimestamp(),
                        )
                        requestRef.set(data)
                            .addOnSuccessListener { onComplete(Result.success(Unit)) }
                            .addOnFailureListener { onComplete(Result.failure(it)) }
                    }
                }
            }.addOnFailureListener { onComplete(Result.failure(it)) }
        }.addOnFailureListener { onComplete(Result.failure(it)) }
    }

    fun respondToRequest(request: ContactRequest, accept: Boolean, onComplete: (Result<Unit>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onComplete(Result.failure(IllegalStateException("Sign in first")))
        val requestRef = firestore.collection("contactRequests").document(request.id)
        if (!accept) {
            requestRef.update("status", "declined")
                .addOnSuccessListener { onComplete(Result.success(Unit)) }
                .addOnFailureListener { onComplete(Result.failure(it)) }
            return
        }
        val pair = listOf(uid, request.senderId).sorted()
        val connection = mapOf(
            "participantIds" to pair,
            "requestId" to request.id,
            "createdAt" to FieldValue.serverTimestamp(),
        )
        firestore.runBatch { batch ->
            batch.update(requestRef, "status", "accepted")
            batch.set(firestore.collection("connections").document(pair.joinToString("_")), connection)
        }.addOnSuccessListener { onComplete(Result.success(Unit)) }
            .addOnFailureListener { onComplete(Result.failure(it)) }
    }

    fun setBlocked(userId: String, blocked: Boolean, onComplete: (Result<Unit>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onComplete(Result.failure(IllegalStateException("Sign in first")))
        val ref = firestore.collection("blocks").document("${uid}_$userId")
        val task = if (blocked) ref.set(mapOf("blockerId" to uid, "blockedUserId" to userId, "createdAt" to FieldValue.serverTimestamp()))
            else ref.delete()
        task.addOnSuccessListener { onComplete(Result.success(Unit)) }
            .addOnFailureListener { onComplete(Result.failure(it)) }
    }

    private object EmptyListener : ListenerRegistration {
        override fun remove() = Unit
    }
}
