package app.hyuabot.backend.presence

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ZSetOperations
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class AdminShuttlePresenceServiceTest {
    private val now = Instant.parse("2026-07-21T03:00:00Z")
    private val redisTemplate = mock<RedisTemplate<String, String>>()
    private val zSetOps = mock<ZSetOperations<String, String>>()
    private val demandWindowService = mock<ShuttleDemandWindowService>()
    private val service =
        AdminShuttlePresenceService(
            redisTemplate,
            demandWindowService,
            Clock.fixed(now, ZoneOffset.UTC),
        )

    @Test
    fun `returns raw demand counts per stop and zero when there is no upcoming departure`() {
        whenever(redisTemplate.opsForZSet()).thenReturn(zSetOps)
        val window = ShuttleDemandWindowService.DemandWindow(startEpoch = 1000L, keyTtlSeconds = 300L)

        // dormitory_o: window present, count 5
        whenever(demandWindowService.demandWindow("dormitory_o", now)).thenReturn(window)
        whenever(zSetOps.count("presence:shuttle:dormitory_o", 1000.0, Double.POSITIVE_INFINITY)).thenReturn(5L)
        // shuttlecock_o: window present, ZCOUNT null -> 0
        whenever(demandWindowService.demandWindow("shuttlecock_o", now)).thenReturn(window)
        whenever(zSetOps.count("presence:shuttle:shuttlecock_o", 1000.0, Double.POSITIVE_INFINITY)).thenReturn(null)
        // station: no upcoming departure -> 0 (no ZCOUNT)
        whenever(demandWindowService.demandWindow("station", now)).thenReturn(null)
        // terminal: window present, count 4
        whenever(demandWindowService.demandWindow("terminal", now)).thenReturn(window)
        whenever(zSetOps.count("presence:shuttle:terminal", 1000.0, Double.POSITIVE_INFINITY)).thenReturn(4L)
        // jungang_stn: no upcoming departure -> 0
        whenever(demandWindowService.demandWindow("jungang_stn", now)).thenReturn(null)
        // shuttlecock_i: window present, count 2
        whenever(demandWindowService.demandWindow("shuttlecock_i", now)).thenReturn(window)
        whenever(zSetOps.count("presence:shuttle:shuttlecock_i", 1000.0, Double.POSITIVE_INFINITY)).thenReturn(2L)

        val response = service.getViewerCounts()

        assertEquals(now, response.updatedAt)
        assertEquals(6, response.stops.size)

        val expected =
            listOf(
                "dormitory_o" to 5L,
                "shuttlecock_o" to 0L,
                "station" to 0L,
                "terminal" to 4L,
                "jungang_stn" to 0L,
                "shuttlecock_i" to 2L,
            )
        expected.forEachIndexed { index, (id, count) ->
            assertEquals(id, response.stops[index].stopId)
            assertEquals(count, response.stops[index].viewerCount)
        }
    }
}
