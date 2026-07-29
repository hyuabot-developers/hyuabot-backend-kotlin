package app.hyuabot.backend.presence

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AdminShuttlePresenceResponseTest {
    @Test
    fun dataClass() {
        val now = Instant.now()
        val response =
            AdminShuttlePresenceResponse(
                stops = listOf(AdminShuttlePresenceResponse.StopViewerCount(stopId = "stop", viewerCount = 3L)),
                updatedAt = now,
                activeWindowSeconds = 60L,
            )
        assertEquals(1, response.stops.size)
        assertEquals(now, response.updatedAt)
        assertEquals(60L, response.activeWindowSeconds)
        assertEquals(now, response.component2())
        assertEquals(60L, response.component3())
        assertEquals(response, response.copy())
        assertEquals(response.hashCode(), response.copy().hashCode())
        assertTrue(response.toString().contains("60"))
        assertNotEquals(response, response.copy(activeWindowSeconds = 30L))

        val stop = response.stops[0]
        assertEquals("stop", stop.stopId)
        assertEquals(3L, stop.viewerCount)
        assertEquals("stop", stop.component1())
        assertEquals(3L, stop.component2())
        assertEquals(stop, stop.copy())
        assertEquals(stop.hashCode(), stop.copy().hashCode())
        assertTrue(stop.toString().contains("stop"))
        assertNotEquals(stop, stop.copy(viewerCount = 9L))
    }
}
