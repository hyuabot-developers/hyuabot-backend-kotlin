package app.hyuabot.backend.liveactivity

import app.hyuabot.backend.liveactivity.domain.ShuttleLiveActivityCheckpointRequest
import app.hyuabot.backend.liveactivity.domain.ShuttleLiveActivityRegisterRequest
import app.hyuabot.backend.liveactivity.domain.ShuttleLiveActivityRegisterResponse
import app.hyuabot.backend.liveactivity.service.ShuttleLiveActivityService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShuttleLiveActivityControllerTest {
    @MockitoBean
    private lateinit var service: ShuttleLiveActivityService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DisplayName("Register Live Activity push token")
    fun register() {
        val request = registerRequest()
        doReturn(ShuttleLiveActivityRegisterResponse("key", 3)).whenever(service).register(request)

        mockMvc
            .perform(
                post("/api/v1/live-activity/shuttle")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.key").value("key"))
            .andExpect(jsonPath("$.scheduledPushCount").value(3))
    }

    @Test
    @DisplayName("Unregister Live Activity push token")
    fun unregister() {
        mockMvc
            .perform(delete("/api/v1/live-activity/shuttle/key"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Live Activity registration removed."))

        verify(service).unregister("key")
    }

    private fun registerRequest() =
        ShuttleLiveActivityRegisterRequest(
            key = "key",
            pushToken = "token",
            apnsEnvironment = "development",
            alarmKind = "boarding",
            titleText = "title",
            statusText = "status",
            dynamicIslandStatusText = "island",
            currentStopName = "current",
            nextStopName = "next",
            checkpointWaitingFormat = "%s waiting",
            checkpointApproachingFormat = "%s approaching",
            checkpointDepartedFormat = "%s departed",
            progressSegments = listOf(100),
            createdAt = Instant.parse("2026-06-21T00:00:00Z"),
            expiresAt = Instant.parse("2026-06-21T00:10:00Z"),
            checkpoints = listOf(ShuttleLiveActivityCheckpointRequest("current", Instant.parse("2026-06-21T00:00:00Z"))),
        )
}
