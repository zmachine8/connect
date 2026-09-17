package ee.ut.connect.data.repository

import ee.ut.connect.data.model.ConnectUser

class FakeUserRepository : UserRepository {
    override suspend fun getUsers(): List<ConnectUser> = sampleUsers
}

val sampleUsers = listOf(
    ConnectUser("raigo", "Raigo", true),
    ConnectUser("mairon", "Mairon", false),
    ConnectUser("robin", "Robin", true),
    ConnectUser("kermo", "Kermo", false),
    ConnectUser("reimo", "Reimo", true),
)
