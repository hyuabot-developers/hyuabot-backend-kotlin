package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InquiryThreadTest {
    private val fixedId: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val otherId: UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")

    private fun create(id: UUID = fixedId) =
        InquiryThread(
            id = id,
            installationId = UUID.fromString("33333333-3333-3333-3333-333333333333"),
            platform = "iOS",
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
        assertFalse(a == create(id = otherId))
    }

    @Test
    fun fieldAccess() {
        val now = ZonedDateTime.now()
        val installation = UUID.fromString("33333333-3333-3333-3333-333333333333")
        val entity =
            InquiryThread(
                id = fixedId,
                installationId = installation,
                platform = "iOS",
                appVersion = "1.0.0",
                status = "PENDING",
                subject = "제목",
                contactEmail = "a@b.com",
                entryScreen = "shuttle",
                entryScreenName = "셔틀",
                assignedAdminUserId = "admin",
                lastMessageAt = now,
                createdAt = now,
                updatedAt = now,
            )
        assertEquals(fixedId, entity.id)
        assertEquals(installation, entity.installationId)
        assertEquals("iOS", entity.platform)
        assertEquals("1.0.0", entity.appVersion)
        assertEquals("PENDING", entity.status)
        assertEquals("제목", entity.subject)
        assertEquals("a@b.com", entity.contactEmail)
        assertEquals("shuttle", entity.entryScreen)
        assertEquals("셔틀", entity.entryScreenName)
        assertEquals("admin", entity.assignedAdminUserId)
        assertEquals(now, entity.lastMessageAt)
        assertEquals(now, entity.createdAt)
        assertEquals(now, entity.updatedAt)
    }

    @Test
    fun noArgConstructor() {
        val entity = InquiryThread::class.java.getDeclaredConstructor().newInstance()
        assertNotNull(entity)
    }
}
