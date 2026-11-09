package ee.ut.connect.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class ConnectGroup(val id: String, val name: String, val memberIds: List<String>, val createdBy: String)
data class GroupMessage(val id: String, val senderId: String, val senderName: String, val text: String, val createdAt: Long)

class GroupRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun observeGroups(onResult: (Result<List<ConnectGroup>>) -> Unit): ListenerRegistration {
        val uid = auth.currentUser?.uid ?: return EmptyGroupListener.also {
            onResult(Result.failure(IllegalStateException("Sign in to see groups")))
        }
        return firestore.collection("groups").whereArrayContains("memberIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) onResult(Result.failure(error))
                else onResult(Result.success(snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val members = (doc.get("memberIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
                    ConnectGroup(doc.id, name, members, doc.getString("createdBy").orEmpty())
                }.sortedBy { it.name.lowercase() }))
            }
    }

    fun createGroup(name: String, callback: (Result<Unit>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return callback(Result.failure(IllegalStateException("Sign in first")))
        val title = name.trim()
        if (title.length !in 2..60) return callback(Result.failure(IllegalArgumentException("Group name must be 2–60 characters.")))
        firestore.collection("groups").add(mapOf(
            "name" to title, "memberIds" to listOf(uid), "createdBy" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
        )).addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun addMember(groupId: String, personId: String, callback: (Result<Unit>) -> Unit) {
        if (personId == auth.currentUser?.uid) return callback(Result.failure(IllegalArgumentException("You are already in this group.")))
        firestore.collection("groups").document(groupId).update("memberIds", FieldValue.arrayUnion(personId))
            .addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun observeMessages(groupId: String, callback: (Result<List<GroupMessage>>) -> Unit): ListenerRegistration =
        firestore.collection("groups").document(groupId).collection("messages")
            .orderBy("createdAt").addSnapshotListener { snapshot, error ->
                if (error != null) callback(Result.failure(error))
                else callback(Result.success(snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val sender = doc.getString("senderId") ?: return@mapNotNull null
                    val text = doc.getString("text") ?: return@mapNotNull null
                    GroupMessage(doc.id, sender, doc.getString("senderName") ?: "Member", text, doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L)
                }))
            }

    fun sendMessage(groupId: String, text: String, callback: (Result<Unit>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return callback(Result.failure(IllegalStateException("Sign in first")))
        val body = text.trim()
        if (body.isEmpty() || body.length > 2000) return callback(Result.failure(IllegalArgumentException("Message must be 1–2000 characters.")))
        firestore.collection("users").document(uid).get().addOnSuccessListener { profile ->
            firestore.collection("groups").document(groupId).collection("messages").add(mapOf(
                "senderId" to uid, "senderName" to (profile.getString("displayName") ?: "Connect user"),
                "text" to body, "createdAt" to FieldValue.serverTimestamp(),
            )).addOnSuccessListener { callback(Result.success(Unit)) }
                .addOnFailureListener { callback(Result.failure(it)) }
        }.addOnFailureListener { callback(Result.failure(it)) }
    }

    private object EmptyGroupListener : ListenerRegistration { override fun remove() = Unit }
}
