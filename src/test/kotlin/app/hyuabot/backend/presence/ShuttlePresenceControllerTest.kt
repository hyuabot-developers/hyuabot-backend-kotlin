package app.hyuabot.backend.presence

import app.hyuabot.backend.utility.ResponseBuilder
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import kotlin.test.assertEquals

class ShuttlePresenceControllerTest {
    private val service = mock<ShuttlePresenceService>()
    private val controller = ShuttlePresenceController(service)
    private val request =
        ShuttlePresenceRequest(
            stopId = "station",
            sessionId = "123e4567-e89b-12d3-a456-426614174000",
            platform = "android",
            appVersion = "5.2.0",
        )

    @Test
    fun `returns the presence response for a valid heartbeat`() {
        val presence =
            ShuttlePresenceResponse(
                viewerCount = 3,
                visible = true,
                updatedAt = Instant.parse("2026-07-21T03:00:00Z"),
                activeWindowSeconds = 75,
            )
        whenever(service.heartbeat(request)).thenReturn(presence)

        val response = controller.heartbeat(request)

        assertEquals(200, response.statusCode.value())
        assertEquals(presence, response.body)
        verify(service).heartbeat(request)
    }

    @Test
    fun `returns bad request for invalid heartbeat dimensions`() {
        doThrow(IllegalArgumentException()).`when`(service).heartbeat(request)

        val response = controller.heartbeat(request)

        assertEquals(400, response.statusCode.value())
        assertEquals("INVALID_SHUTTLE_PRESENCE", (response.body as ResponseBuilder.Message).message)
    }
}
