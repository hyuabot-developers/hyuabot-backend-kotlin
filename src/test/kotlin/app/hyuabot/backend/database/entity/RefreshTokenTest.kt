package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RefreshTokenTest {
    private val uuidA = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val uuidB = UUID.fromString("00000000-0000-0000-0000-000000000002")

    private fun create(uuid: UUID = uuidA) =
        RefreshToken(
            uuid = uuid,
            userID = "x",
            refreshToken = "x",
            expiredAt = ZonedDateTime.now(),
            createdAt = ZonedDateTime.now(),
            updatedAt = ZonedDateTime.now(),
            user = null,
        )

    @Test
    fun equalsAndHashCode() {
        val a = create()
        val nullValue: Any? = null
        assertTrue(a == a)
        assertFalse(a.equals(nullValue))
        assertFalse(a.equals(Any()))
        assertEquals(a, create())
        assertEquals(a.hashCode(), create().hashCode())
        assertFalse(a == create(uuid = uuidB))
    }

    @Test
    fun mutators() {
        val e = create()
        e.userID = "y"
        e.refreshToken = "y"
        e.expiredAt = ZonedDateTime.now()
        e.createdAt = ZonedDateTime.now()
        e.updatedAt = ZonedDateTime.now()
    }
}
