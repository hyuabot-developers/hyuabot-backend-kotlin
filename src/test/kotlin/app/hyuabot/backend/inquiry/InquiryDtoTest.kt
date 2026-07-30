package app.hyuabot.backend.inquiry

import app.hyuabot.backend.inquiry.domain.PatchThreadRequest
import app.hyuabot.backend.inquiry.domain.RegisterPushTokenRequest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class InquiryDtoTest {
    @Test
    fun patchThreadRequestDefaults() {
        assertEquals(null, PatchThreadRequest().status)
    }

    @Test
    fun registerPushTokenRequestDataMembers() {
        val request = RegisterPushTokenRequest(provider = "FCM", token = "token", platform = "ANDROID")
        assertEquals("FCM", request.component1())
        assertEquals("token", request.component2())
        assertEquals("ANDROID", request.component3())
        assertEquals(request, request.copy())
        assertEquals(request.hashCode(), request.copy().hashCode())
        assertNotEquals(request, request.copy(token = "other"))
        assertEquals("RegisterPushTokenRequest(provider=FCM, token=token, platform=ANDROID)", request.toString())
    }
}
