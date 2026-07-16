package app.hyuabot.backend.adminpush

import app.hyuabot.backend.adminpush.domain.AdminPushPublicKeyResponse
import app.hyuabot.backend.adminpush.domain.AdminPushSubscriptionStatusResponse
import app.hyuabot.backend.security.AdminPermission
import app.hyuabot.backend.security.WithCustomMockUser
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestClientException

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminPushControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var notifierClient: NotifierClient

    @Test
    @WithCustomMockUser(username = "jil8885")
    fun `super admin manages this device subscription`() {
        whenever(notifierClient.getPublicKey()).thenReturn(AdminPushPublicKeyResponse("public-key"))
        whenever(notifierClient.getStatus("jil8885", "https://push.example/device"))
            .thenReturn(AdminPushSubscriptionStatusResponse(true))

        mockMvc
            .perform(get("/api/v1/user/push/public-key"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.publicKey").value("public-key"))
        mockMvc
            .perform(
                get("/api/v1/user/push/status")
                    .queryParam("endpoint", "https://push.example/device"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.enabled").value(true))
        mockMvc
            .perform(
                post("/api/v1/user/push/subscription")
                    .header("User-Agent", "Safari")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"endpoint":"https://push.example/device","keys":{"p256dh":"key","auth":"auth"}}""",
                    ),
            ).andExpect(status().isNoContent)
        mockMvc
            .perform(
                delete("/api/v1/user/push/subscription")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"endpoint":"https://push.example/device"}"""),
            ).andExpect(status().isNoContent)

        verify(notifierClient).subscribe(eq("jil8885"), eq("Safari"), any())
        verify(notifierClient).unsubscribe("jil8885", "https://push.example/device")
    }

    @Test
    @WithCustomMockUser
    fun `notifier failure returns service unavailable`() {
        doThrow(RestClientException("unavailable")).whenever(notifierClient).getPublicKey()

        mockMvc
            .perform(get("/api/v1/user/push/public-key"))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.enabled").value(false))
    }

    @Test
    @WithCustomMockUser(permissions = [AdminPermission.SHUTTLE])
    fun `regular administrator cannot manage operational push`() {
        mockMvc.perform(get("/api/v1/user/push/public-key")).andExpect(status().isForbidden)
    }

    @Test
    fun `anonymous user cannot manage operational push`() {
        mockMvc.perform(get("/api/v1/user/push/public-key")).andExpect(status().isUnauthorized)
    }
}
