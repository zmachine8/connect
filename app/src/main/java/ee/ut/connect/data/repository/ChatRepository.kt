package ee.ut.connect.data.repository

import ee.ut.connect.data.model.ChatMessage

interface ChatRepository {
    suspend fun getMessages(userId: String): List<ChatMessage>
    suspend fun sendMessage(userId: String, text: String)
}
