package ee.ut.connect.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.installations.FirebaseInstallations
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger

object PushInstallationRepository {
    private val firestore = FirebaseFirestore.getInstance()

    fun save(uid: String, installationId: String) {
        val id = documentId(installationId)
        firestore.collection("users").document(uid)
            .collection("fcmInstallations").document(id)
            .set(mapOf("installationId" to installationId, "updatedAt" to FieldValue.serverTimestamp()))
    }

    /** Remove this device's push registration before Firebase Auth signs the user out. */
    fun removeCurrentInstallation(uid: String, onComplete: () -> Unit) {
        val userRef = firestore.collection("users").document(uid)
        val remaining = AtomicInteger(2)
        val finishOne = { if (remaining.decrementAndGet() == 0) onComplete() }

        FirebaseInstallations.getInstance().id
            .addOnSuccessListener { installationId ->
                userRef.collection("fcmInstallations").document(documentId(installationId)).delete()
                    .addOnCompleteListener { finishOne() }
            }
            .addOnFailureListener { finishOne() }

        // Clean up registrations written by app versions that used deprecated FCM tokens.
        userRef.collection("fcmTokens").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    finishOne()
                } else {
                    firestore.batch().also { batch -> snapshot.documents.forEach { batch.delete(it.reference) } }
                        .commit().addOnCompleteListener { finishOne() }
                }
            }
            .addOnFailureListener { finishOne() }
    }

    private fun documentId(installationId: String): String =
        MessageDigest.getInstance("SHA-256").digest(installationId.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
