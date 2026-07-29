package app.hyuabot.backend.inquiry

import app.hyuabot.backend.inquiry.domain.PatchThreadRequest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InquiryDtoTest {
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
