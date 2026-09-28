package ee.ut.connect.data.model

data class ChatMessage(
    val id: String,
    val senderId: String,
    val text: String,
    val imageBytes: ByteArray? = null,
    val documentName: String? = null,
    val documentBytes: ByteArray? = null,
    val documentMimeType: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)
