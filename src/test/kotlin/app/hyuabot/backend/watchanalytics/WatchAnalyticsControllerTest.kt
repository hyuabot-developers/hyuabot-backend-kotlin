package app.hyuabot.backend.watchanalytics

import app.hyuabot.backend.utility.ResponseBuilder
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.assertEquals

class WatchAnalyticsControllerTest {
    private val service = mock<WatchAnalyticsService>()
    private val controller = WatchAnalyticsController(service)
    private val request =
        WatchAnalyticsEventRequest(
            event = "watch_app_open",
            platform = "watchos",
            installationId = "123e4567-e89b-12d3-a456-426614174000",
            appVersion = "26.7.13",
            entryPoint = "app",
        )

    @Test
    fun `accepts a valid event`() {
        val response = controller.recordEvent(request)

        assertEquals(202, response.statusCode.value())
        verify(service).record(request)
    }

    @Test
    fun `returns bad request for an invalid event`() {
        doThrow(IllegalArgumentException()).`when`(service).record(request)

        val response = controller.recordEvent(request)

        assertEquals(400, response.statusCode.value())
        assertEquals("INVALID_WATCH_ANALYTICS_EVENT", (response.body as ResponseBuilder.Message).message)
    }
}
