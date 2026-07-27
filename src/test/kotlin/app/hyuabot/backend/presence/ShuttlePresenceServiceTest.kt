package app.hyuabot.backend.presence

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShuttlePresenceServiceTest {
    private val now = Instant.parse("2026-07-21T03:00:00Z")
    private val redisTemplate = mock<RedisTemplate<String, String>>()
    private val meterRegistry = SimpleMeterRegistry()
    private val demandWindowService = mock<ShuttleDemandWindowService>()
    private val service =
        ShuttlePresenceService(
            redisTemplate,
            meterRegistry,
            Clock.fixed(now, ZoneOffset.UTC),
            demandWindowService,
        )

    @Test
    fun `records a valid heartbeat and returns the demand count until the next departure`() {
        whenever(demandWindowService.demandWindow("station", now))
            .thenReturn(
                ShuttleDemandWindowService.DemandWindow(
                    startEpoch = now.epochSecond - 600,
                    keyTtlSeconds = 300,
                ),
            )
        whenever(
            redisTemplate.execute(
                any<RedisScript<Long>>(),
                any<List<String>>(),
                any<String>(),
                any<String>(),
                any<String>(),
                any<String>(),
                any<String>(),
                any<String>(),
                any<String>(),
            ),
        ).thenReturn(3L).thenReturn(null)

        val response = service.heartbeat(validRequest())

        assertTrue(response.visible)
        assertEquals(3, response.viewerCount)
        assertEquals(now, response.updatedAt)
        assertEquals(600, response.activeWindowSeconds)
        assertEquals(
            1.0,
            meterRegistry
                .counter(
                    "hyuabot.shuttle.presence.heartbeats",
                    "platform",
                    "android",
                    "stop_id",
                    "station",
                ).count(),
        )

        val fallbackResponse = service.heartbeat(validRequest())
        assertFalse(fallbackResponse.visible)
        assertNull(fallbackResponse.viewerCount)
    }

    @Test
    fun `returns an empty demand when there is no upcoming departure`() {
        whenever(demandWindowService.demandWindow("station", now)).thenReturn(null)

        val response = service.heartbeat(validRequest())

        assertFalse(response.visible)
        assertNull(response.viewerCount)
        assertEquals(0, response.activeWindowSeconds)
        assertEquals(
            1.0,
            meterRegistry
                .counter(
                    "hyuabot.shuttle.presence.heartbeats",
                    "platform",
                    "android",
                    "stop_id",
                    "station",
                ).count(),
        )
    }

    @Test
    fun `hides small groups and exposes counts from three viewers`() {
        val smallGroup = service.responseFor(2, now, 900)
        assertFalse(smallGroup.visible)
        assertNull(smallGroup.viewerCount)

        val visibleGroup = service.responseFor(3, now, 900)
        assertTrue(visibleGroup.visible)
        assertEquals(3, visibleGroup.viewerCount)
        assertEquals(900, visibleGroup.activeWindowSeconds)
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
