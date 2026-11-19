package ee.ut.connect.data.model

/** User data shown in people lists and conversations. */
data class ConnectUser(
    val id: String,
    val displayName: String,
    val isOnline: Boolean?,
    val username: String = "",
    val unreadCount: Int = 0,
)
