package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DevicePushTokenTest {
    private val installationId: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")

    private fun create(id: Long? = 1L) =
        DevicePushToken(
            id = id,
            installationId = installationId,
            platform = "IOS",
            provider = "APNS",
            token = "token",
            createdAt = ZonedDateTime.now(),
            updatedAt = ZonedDateTime.now(),
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
        assertFalse(a == create(id = 9999L))
        assertFalse(create(id = null) == create(id = null))
        assertEquals(0, create(id = null).hashCode())
    }

    @Test
    fun fieldAccess() {
        val now = ZonedDateTime.now()
        val other = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val entity = create()
        entity.id = 42L
        entity.installationId = other
        entity.platform = "ANDROID"
        entity.provider = "FCM"
        entity.token = "new-token"
        entity.createdAt = now
        entity.updatedAt = now
        assertEquals(42L, entity.id)
        assertEquals(other, entity.installationId)
        assertEquals("ANDROID", entity.platform)
        assertEquals("FCM", entity.provider)
        assertEquals("new-token", entity.token)
        assertEquals(now, entity.createdAt)
        assertEquals(now, entity.updatedAt)
    }
}
