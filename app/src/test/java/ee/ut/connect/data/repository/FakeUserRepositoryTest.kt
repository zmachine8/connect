package ee.ut.connect.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeUserRepositoryTest {
    @Test
    fun `fake repository returns five unique users`() = runBlocking {
        val users = FakeUserRepository().getUsers()
        assertEquals(5, users.size)
        assertEquals(users.size, users.map { it.id }.distinct().size)
        assertTrue(users.all { it.displayName.isNotBlank() })
    }
}
