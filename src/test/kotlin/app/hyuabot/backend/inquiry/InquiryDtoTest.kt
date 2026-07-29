package app.hyuabot.backend.inquiry

import app.hyuabot.backend.inquiry.domain.InquiryEvent
import app.hyuabot.backend.inquiry.domain.MessageResponse
import app.hyuabot.backend.inquiry.domain.PatchThreadRequest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InquiryDtoTest {
    @Test
    fun inquiryEvent() {
        val message =
            MessageResponse(
                id = 1L,
                senderType = "USER",
                body = "hi",
                readAt = null,
                createdAt = "2026-07-29T12:00:00+09:00",
            )
        val event =
            InquiryEvent(
                kind = "message",
                threadId = "thread",
                installationId = "install",
                message = message,
                reader = "USER",
                status = "OPEN",
            )
        assertEquals("message", event.kind)
        assertEquals("thread", event.threadId)
        assertEquals("install", event.installationId)
        assertEquals(message, event.message)
        assertEquals("USER", event.reader)
        assertEquals("OPEN", event.status)
        assertEquals("message", event.component1())
        assertEquals("thread", event.component2())
        assertEquals("install", event.component3())
        assertEquals(message, event.component4())
        assertEquals("USER", event.component5())
        assertEquals("OPEN", event.component6())
        assertEquals(event, event.copy())
        assertEquals(event.hashCode(), event.copy().hashCode())
        assertEquals("thread2", event.copy(threadId = "thread2").threadId)
        assertTrue(event.toString().contains("message"))

        val defaults = InquiryEvent(kind = "read", threadId = "t", installationId = "i")
        assertNull(defaults.message)
        assertNull(defaults.reader)
        assertNull(defaults.status)
        assertNotEquals(event, defaults)
    }

    @Test
    fun patchThreadRequest() {
        val request = PatchThreadRequest(status = "PENDING", assignedAdminUserId = "admin")
        assertEquals("PENDING", request.status)
        assertEquals("admin", request.assignedAdminUserId)
        assertEquals("PENDING", request.component1())
        assertEquals("admin", request.component2())
        assertEquals("other", request.copy(assignedAdminUserId = "other").assignedAdminUserId)
        assertEquals(request, request.copy())
        assertEquals(request.hashCode(), request.copy().hashCode())
        assertTrue(request.toString().contains("PENDING"))

        val defaults = PatchThreadRequest()
        assertNull(defaults.status)
        assertNull(defaults.assignedAdminUserId)
        assertNotEquals(request, defaults)
    }
}
