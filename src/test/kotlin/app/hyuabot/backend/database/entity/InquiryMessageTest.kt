package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InquiryMessageTest {
    private val threadId: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")

    private fun create(id: Long? = 1L) =
        InquiryMessage(
            id = id,
            threadId = threadId,
            senderType = "USER",
            body = "hello",
            createdAt = ZonedDateTime.now(),
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
        assertFalse(a == create(id = 2L))
        assertEquals(0, create(id = null).hashCode())
    }

    @Test
    fun fieldAccess() {
        val now = ZonedDateTime.now()
        val entity =
            InquiryMessage(
                id = 1L,
                threadId = threadId,
                senderType = "ADMIN",
                senderAdminUserId = "admin",
                body = "hello",
                readAt = now,
                createdAt = now,
            )
        assertEquals(1L, entity.id)
        assertEquals(threadId, entity.threadId)
        assertEquals("ADMIN", entity.senderType)
        assertEquals("admin", entity.senderAdminUserId)
        assertEquals("hello", entity.body)
        assertEquals(now, entity.readAt)
        assertEquals(now, entity.createdAt)
    }

    @Test
    fun noArgConstructor() {
        val entity = InquiryMessage::class.java.getDeclaredConstructor().newInstance()
        assertNotNull(entity)
    }

    @Test
    fun mutableProperties() {
        val now = ZonedDateTime.now()
        val other = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val entity = create()
        entity.id = 99L
        entity.threadId = other
        entity.senderType = "ADMIN"
        entity.senderAdminUserId = "admin"
        entity.body = "답변"
        entity.readAt = now
        entity.createdAt = now
        assertEquals(99L, entity.id)
        assertEquals(other, entity.threadId)
        assertEquals("ADMIN", entity.senderType)
        assertEquals("admin", entity.senderAdminUserId)
        assertEquals("답변", entity.body)
        assertEquals(now, entity.readAt)
        assertEquals(now, entity.createdAt)
    }
}
