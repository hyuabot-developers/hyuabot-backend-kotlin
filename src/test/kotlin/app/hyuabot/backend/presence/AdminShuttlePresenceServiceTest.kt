package app.hyuabot.backend.presence

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ZSetOperations
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class AdminShuttlePresenceServiceTest {
    @Test
    fun `getViewerCounts returns raw counts for every stop and treats null as zero`() {
        val now = Instant.parse("2026-07-21T03:00:00Z")
        val clock = Clock.fixed(now, ZoneOffset.UTC)
        val redisTemplate = mock<RedisTemplate<String, String>>()
        val zSetOps = mock<ZSetOperations<String, String>>()

        whenever(redisTemplate.opsForZSet()).thenReturn(zSetOps)

        val counts = listOf(5L, null, 3L, 0L, 1L, 2L)
        var callIndex = 0
        whenever(zSetOps.count(any<String>(), any(), any())).thenAnswer { counts[callIndex++] }

        val service = AdminShuttlePresenceService(redisTemplate, clock)
        val response = service.getViewerCounts()

        assertEquals(now, response.updatedAt)
        assertEquals(75L, response.activeWindowSeconds)
        assertEquals(6, response.stops.size)

        val expectedStops =
            listOf(
                "dormitory_o" to 5L,
                "shuttlecock_o" to 0L,
                "station" to 3L,
                "terminal" to 0L,
                "jungang_stn" to 1L,
                "shuttlecock_i" to 2L,
            )
        expectedStops.forEachIndexed { index, (id, count) ->
            assertEquals(id, response.stops[index].stopId)
            assertEquals(count, response.stops[index].viewerCount)
        }
    }

    @Test
    fun `exposes the six shuttle stop ids in display order`() {
        assertEquals(
            listOf(
                "dormitory_o",
                "shuttlecock_o",
                "station",
                "terminal",
                "jungang_stn",
                "shuttlecock_i",
            ),
            AdminShuttlePresenceService.SHUTTLE_STOP_IDS,
        )
    }
}
