package ee.ut.connect.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest

object PushTokenRepository {
    fun save(uid: String, token: String) {
        val id = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
            .joinToString("") { "%02x".format(it) }
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("fcmTokens").document(id)
            .set(mapOf("token" to token, "updatedAt" to FieldValue.serverTimestamp()))
    }

    fun remove(uid: String, token: String) {
        val id = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
            .joinToString("") { "%02x".format(it) }
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("fcmTokens").document(id).delete()
    }
}
