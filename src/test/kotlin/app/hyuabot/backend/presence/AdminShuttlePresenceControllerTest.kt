package app.hyuabot.backend.presence

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import java.time.Instant
import kotlin.test.assertEquals

class AdminShuttlePresenceControllerTest {
    @Test
    fun `getViewerCounts returns OK with the service response`() {
        val sampleUpdatedAt = Instant.parse("2026-07-21T03:00:00Z")
        val sampleStop = AdminShuttlePresenceResponse.StopViewerCount("station", 4L)
        val sample = AdminShuttlePresenceResponse(listOf(sampleStop), sampleUpdatedAt, 75L)

        val service = mock<AdminShuttlePresenceService>()
        whenever(service.getViewerCounts()).thenReturn(sample)

        val controller = AdminShuttlePresenceController(service)
        val response = controller.getViewerCounts()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(sample, response.body)
    }
}
