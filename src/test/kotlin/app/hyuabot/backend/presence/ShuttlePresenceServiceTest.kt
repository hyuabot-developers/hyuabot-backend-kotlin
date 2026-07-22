package app.hyuabot.backend.presence

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.springframework.data.redis.core.RedisTemplate
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShuttlePresenceServiceTest {
    private val now = Instant.parse("2026-07-21T03:00:00Z")
    private val service =
        ShuttlePresenceService(
            mock<RedisTemplate<String, String>>(),
            SimpleMeterRegistry(),
            Clock.fixed(now, ZoneOffset.UTC),
        )

    @Test
    fun `hides small groups and exposes counts from three viewers`() {
        val smallGroup = service.responseFor(2, now)
        assertFalse(smallGroup.visible)
        assertNull(smallGroup.viewerCount)

        val visibleGroup = service.responseFor(3, now)
        assertTrue(visibleGroup.visible)
        assertEquals(3, visibleGroup.viewerCount)
        assertEquals(75, visibleGroup.activeWindowSeconds)
    }

    @Test
    fun `rejects unsupported heartbeat dimensions before writing redis`() {
        assertThrows<IllegalArgumentException> {
            service.heartbeat(validRequest(stopId = "unknown"))
        }
        assertThrows<IllegalArgumentException> {
            service.heartbeat(validRequest(platform = "web"))
        }
        assertThrows<IllegalArgumentException> {
            service.heartbeat(validRequest(sessionId = "not-a-uuid"))
        }
        assertThrows<IllegalArgumentException> {
            service.heartbeat(validRequest(appVersion = "invalid version"))
        }
    }

    private fun validRequest(
        stopId: String = "station",
        sessionId: String = "123e4567-e89b-12d3-a456-426614174000",
        platform: String = "android",
        appVersion: String = "5.2.0",
    ) = ShuttlePresenceRequest(stopId, sessionId, platform, appVersion)
}
