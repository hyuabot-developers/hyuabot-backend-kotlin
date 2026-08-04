package app.hyuabot.backend.watchanalytics

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.HyperLogLogOperations
import org.springframework.data.redis.core.RedisTemplate
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals

class WatchAnalyticsServiceTest {
    private val redisTemplate = mock<RedisTemplate<String, String>>()
    private val hyperLogLogOperations = mock<HyperLogLogOperations<String, String>>()
    private val meterRegistry = SimpleMeterRegistry()
    private val clock = Clock.fixed(Instant.parse("2026-07-13T03:00:00Z"), ZoneOffset.UTC)
    private val service =
        WatchAnalyticsService(redisTemplate, meterRegistry, clock).also {
            whenever(redisTemplate.opsForHyperLogLog()).thenReturn(hyperLogLogOperations)
        }

    @Test
    fun `records an app open and active installation`() {
        service.record(validRequest(event = "watch_app_open"))

        assertEquals(
            1.0,
            meterRegistry
                .counter(
                    "hyuabot.watch.events",
                    "event",
                    "watch_app_open",
                    "platform",
                    "wear_os",
                    "entry_point",
                    "app",
                    "stop_id",
                    "none",
                ).count(),
        )
        verify(hyperLogLogOperations).add(
            "analytics:watch:active:wear_os:2026-07-13",
            "wear_os:123e4567-e89b-12d3-a456-426614174000",
        )
        verify(redisTemplate).expire(
            "analytics:watch:active:wear_os:2026-07-13",
            Duration.ofDays(35),
        )
    }

    @Test
    fun `records a selected stop`() {
        service.record(
            validRequest(
                event = "watch_stop_selected",
                entryPoint = "tile",
                stopId = "station",
            ),
        )

        assertEquals(
            1.0,
            meterRegistry
                .counter(
                    "hyuabot.watch.events",
                    "event",
                    "watch_stop_selected",
                    "platform",
                    "wear_os",
                    "entry_point",
                    "tile",
                    "stop_id",
                    "station",
                ).count(),
        )
    }

    @Test
    fun `rejects stop selection without a stop`() {
        assertThrows<IllegalArgumentException> {
            service.record(validRequest(event = "watch_stop_selected"))
        }
    }

    @Test
    fun `rejects unsupported dimensions`() {
        assertThrows<IllegalArgumentException> {
            service.record(validRequest(platform = "android"))
        }
        assertThrows<IllegalArgumentException> {
            service.record(validRequest(event = "other"))
        }
        assertThrows<IllegalArgumentException> {
            service.record(validRequest(entryPoint = "widget"))
        }
        assertThrows<IllegalArgumentException> {
            service.record(validRequest(event = "watch_stop_selected", stopId = "other"))
        }
        assertThrows<IllegalArgumentException> {
            service.record(validRequest(appVersion = "invalid version"))
        }
    }

    @Test
    fun `builds rolling keys for one platform and all platforms`() {
        val today = LocalDate.of(2026, 7, 13)

        val watchOsKeys = service.activeInstallationKeys("watchos", today)
        val allKeys = service.activeInstallationKeys("all", today)

        assertEquals(28, watchOsKeys.size)
        assertEquals("analytics:watch:active:watchos:2026-07-13", watchOsKeys.first())
        assertEquals("analytics:watch:active:watchos:2026-06-16", watchOsKeys.last())
        assertEquals(56, allKeys.size)
    }

    @Test
    fun `gauge starts at zero before the scheduled refresh runs`() {
        listOf("watchos", "wear_os", "all").forEach { platform ->
            assertEquals(
                0.0,
                meterRegistry
                    .get("hyuabot.watch.active.installations")
                    .tag("platform", platform)
                    .tag("window", "28d")
                    .gauge()
                    .value(),
            )
        }
        verify(hyperLogLogOperations, never()).size(
            *service.activeInstallationKeys("watchos", LocalDate.of(2026, 7, 13)).toTypedArray(),
        )
    }

    @Test
    fun `refreshes gauges from redis on schedule`() {
        val today = LocalDate.of(2026, 7, 13)
        val watchosKeys = service.activeInstallationKeys("watchos", today).toTypedArray()
        val wearOsKeys = service.activeInstallationKeys("wear_os", today).toTypedArray()
        val allKeys = service.activeInstallationKeys("all", today).toTypedArray()
        whenever(hyperLogLogOperations.size(*watchosKeys)).thenReturn(7L)
        whenever(hyperLogLogOperations.size(*wearOsKeys)).thenReturn(7L)
        whenever(hyperLogLogOperations.size(*allKeys)).thenReturn(7L)

        service.refreshActiveInstallationCounts()

        listOf("watchos", "wear_os", "all").forEach { platform ->
            assertEquals(
                7.0,
                meterRegistry
                    .get("hyuabot.watch.active.installations")
                    .tag("platform", platform)
                    .tag("window", "28d")
                    .gauge()
                    .value(),
            )
        }
        verify(hyperLogLogOperations).size(*watchosKeys)
        verify(hyperLogLogOperations).size(*wearOsKeys)
        verify(hyperLogLogOperations).size(*allKeys)
    }

    private fun validRequest(
        event: String = "watch_app_open",
        platform: String = "wear_os",
        appVersion: String = "5.1.9",
        entryPoint: String = "app",
        stopId: String? = null,
    ) = WatchAnalyticsEventRequest(
        event = event,
        platform = platform,
        installationId = "123e4567-e89b-12d3-a456-426614174000",
        appVersion = appVersion,
        entryPoint = entryPoint,
        stopId = stopId,
    )
}
