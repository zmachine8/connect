package ee.ut.connect.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

/** Per-conversation read position stored for the signed-in user. */
data class UnreadMarker(val count: Int, val readAtMillis: Long)
/** Minimal latest-message data used to derive unread badges. */
data class LatestConversationMessage(val senderId: String, val createdAtMillis: Long)

/** Observes and updates per-user unread conversation markers. */
class UnreadRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun observe(onMarkers: (Map<String, UnreadMarker>) -> Unit): ListenerRegistration {
        val uid = auth.currentUser?.uid ?: return EmptyListener
        return firestore.collection("users").document(uid).collection("unreadChats")
            .addSnapshotListener { snapshot, _ ->
                onMarkers(snapshot?.documents.orEmpty().associate { doc ->
                    doc.id to UnreadMarker(
                        doc.getLong("count")?.toInt() ?: 0,
                        doc.getTimestamp("readAt")?.toDate()?.time ?: 0L,
                    )
                })
            }
    }

    fun observeLatestDirect(chatId: String, onLatest: (LatestConversationMessage?) -> Unit): ListenerRegistration =
        observeLatest(firestore.collection("chats").document(chatId).collection("messages"), onLatest)

    fun observeLatestGroup(groupId: String, onLatest: (LatestConversationMessage?) -> Unit): ListenerRegistration =
        observeLatest(firestore.collection("groups").document(groupId).collection("messages"), onLatest)

    private fun observeLatest(
        messages: com.google.firebase.firestore.CollectionReference,
        onLatest: (LatestConversationMessage?) -> Unit,
    ): ListenerRegistration = messages.orderBy("createdAt", Query.Direction.DESCENDING).limit(1)
        .addSnapshotListener { snapshot, _ ->
            val latest = snapshot?.documents?.firstOrNull()
            val senderId = latest?.getString("senderId")
            val createdAt = latest?.getTimestamp("createdAt")
            onLatest(if (senderId != null && createdAt != null) {
                LatestConversationMessage(senderId, createdAt.toDate().time)
            } else null)
        }

    fun directKey(chatId: String) = "direct_$chatId"
    fun groupKey(groupId: String) = "group_$groupId"

    fun markDirectRead(chatId: String) = markRead(directKey(chatId), "direct", chatId)
    fun markGroupRead(groupId: String) = markRead(groupKey(groupId), "group", groupId)

    private fun markRead(key: String, type: String, id: String) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).collection("unreadChats").document(key)
            .set(mapOf("count" to 0, "type" to type, "conversationId" to id, "readAt" to FieldValue.serverTimestamp()))
    }

    private object EmptyListener : ListenerRegistration {
        override fun remove() = Unit
    }
}
