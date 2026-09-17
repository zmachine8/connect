package ee.ut.connect.data.repository

import ee.ut.connect.data.model.ChatMessage

class FakeChatRepository : ChatRepository {
    override suspend fun getMessages(userId: String): List<ChatMessage> = listOf(
        ChatMessage("welcome", userId, "Hi! This is a local placeholder chat."),
    )

    override suspend fun sendMessage(userId: String, text: String) {
        // Persistence is intentionally deferred until the Firebase stage.
    }
}
