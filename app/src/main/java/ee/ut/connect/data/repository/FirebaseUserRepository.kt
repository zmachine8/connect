package ee.ut.connect.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import ee.ut.connect.data.model.ConnectUser

class FirebaseUserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun observeUsers(onResult: (Result<List<ConnectUser>>) -> Unit): ListenerRegistration {
        val myId = auth.currentUser?.uid
        return firestore.collection("users").addSnapshotListener { snapshot, error ->
            if (auth.currentUser?.uid != myId) return@addSnapshotListener
            if (error != null) {
                onResult(Result.failure(error))
            } else if (snapshot != null) {
                val users = snapshot.documents.mapNotNull { document ->
                    val name = document.getString("displayName")
                    if (document.id == myId || name.isNullOrBlank()) null
                    else ConnectUser(document.id, name, false)
                }.sortedBy { it.displayName.lowercase() }
                onResult(Result.success(users))
            }
        }
    }
}
