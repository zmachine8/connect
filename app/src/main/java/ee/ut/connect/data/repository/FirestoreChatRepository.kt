package ee.ut.connect.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Blob
import ee.ut.connect.data.model.ChatMessage

class FirestoreChatRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private fun ids(otherId: String): List<String>? {
        val me = auth.currentUser?.uid ?: return null
        return listOf(me, otherId).sorted()
    }

    fun open(otherId: String, callback: (Result<String>) -> Unit) {
        val participants = ids(otherId)
        if (participants == null || participants[0] == participants[1]) {
            callback(Result.failure(IllegalStateException("Sign in and select another user")))
            return
        }
        val chatId = participants.joinToString("_")
        val doc = db.collection("chats").document(chatId)
        db.runTransaction { transaction ->
            val existing = transaction.get(doc)
            if (!existing.exists()) {
                transaction.set(doc, mapOf(
                    "participantIds" to participants,
                    "createdAt" to FieldValue.serverTimestamp(),
                ))
            }
            chatId
        }.addOnSuccessListener { callback(Result.success(it)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun observe(chatId: String, callback: (Result<List<ChatMessage>>) -> Unit): ListenerRegistration =
        db.collection("chats").document(chatId).collection("messages")
            .orderBy("createdAt").limitToLast(30)
            .addSnapshotListener { snapshot, error ->
                if (error != null) callback(Result.failure(error))
                else if (snapshot != null) callback(Result.success(snapshot.documents.mapNotNull { doc ->
                    val senderId = doc.getString("senderId") ?: return@mapNotNull null
                    ChatMessage(doc.id, senderId, doc.getString("text").orEmpty(), doc.getBlob("imageBytes")?.toBytes(),
                        doc.getString("documentName"), doc.getBlob("documentBytes")?.toBytes(),
                        doc.getString("documentMimeType"), doc.getDouble("latitude"), doc.getDouble("longitude"))
                }))
            }

    fun send(chatId: String, text: String, callback: (Result<Unit>) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null || text.isBlank() || text.length > 2000) {
            callback(Result.failure(IllegalArgumentException("Message must be 1 to 2000 characters")))
            return
        }
        db.collection("chats").document(chatId).collection("messages").add(mapOf(
            "senderId" to uid,
            "type" to "text",
            "text" to text,
            "createdAt" to FieldValue.serverTimestamp(),
        )).addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun sendPhoto(chatId: String, jpeg: ByteArray, caption: String, callback: (Result<Unit>) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null || jpeg.isEmpty() || jpeg.size > 200_000 || caption.length > 2000) {
            callback(Result.failure(IllegalArgumentException("Photo must be under 200 KB")))
            return
        }
        db.collection("chats").document(chatId).collection("messages").add(mapOf(
            "senderId" to uid,
            "type" to "image",
            "imageBytes" to Blob.fromBytes(jpeg),
            "text" to caption,
            "createdAt" to FieldValue.serverTimestamp(),
        )).addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun sendDocument(chatId: String, name: String, mimeType: String, bytes: ByteArray,
                     caption: String, callback: (Result<Unit>) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null || name.isBlank() || name.length > 120 || mimeType.length > 100 ||
            bytes.isEmpty() || bytes.size > 200_000 || caption.length > 2000) {
            callback(Result.failure(IllegalArgumentException("Document must be 1–200 KB; name and caption must be short")))
            return
        }
        db.collection("chats").document(chatId).collection("messages").add(mapOf(
            "senderId" to uid, "type" to "document", "text" to caption,
            "documentName" to name, "documentMimeType" to mimeType,
            "documentBytes" to Blob.fromBytes(bytes),
            "createdAt" to FieldValue.serverTimestamp(),
        )).addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }

    fun sendLocation(chatId: String, latitude: Double, longitude: Double,
                     callback: (Result<Unit>) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null || !latitude.isFinite() || !longitude.isFinite() ||
            latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            callback(Result.failure(IllegalArgumentException("Location is unavailable")))
            return
        }
        db.collection("chats").document(chatId).collection("messages").add(mapOf(
            "senderId" to uid, "type" to "location", "latitude" to latitude,
            "longitude" to longitude, "createdAt" to FieldValue.serverTimestamp(),
        )).addOnSuccessListener { callback(Result.success(Unit)) }
            .addOnFailureListener { callback(Result.failure(it)) }
    }
}
