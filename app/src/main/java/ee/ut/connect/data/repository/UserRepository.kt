package ee.ut.connect.data.repository

import ee.ut.connect.data.model.ConnectUser

interface UserRepository {
    suspend fun getUsers(): List<ConnectUser>
}
