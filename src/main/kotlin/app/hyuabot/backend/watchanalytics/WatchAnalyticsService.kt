package app.hyuabot.backend.watchanalytics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class WatchAnalyticsService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val meterRegistry: MeterRegistry,
    private val watchAnalyticsClock: Clock,
) {
    private val activeInstallationCounts = ConcurrentHashMap<String, Double>()

    init {
        val platforms = Platform.entries.map { it.value } + ALL_PLATFORMS
        platforms.forEach { activeInstallationCounts[it] = 0.0 }
        platforms.forEach { platform -> registerActiveInstallationGauge(platform) }
    }

    @Scheduled(fixedDelay = ACTIVE_INSTALLATION_REFRESH_INTERVAL_MS, initialDelay = 0)
    internal fun refreshActiveInstallationCounts() {
        activeInstallationCounts.keys.forEach { platform ->
            activeInstallationCounts[platform] =
                redisTemplate.opsForHyperLogLog().size(*activeInstallationKeys(platform).toTypedArray()).toDouble()
        }
    }

    fun record(request: WatchAnalyticsEventRequest) {
        val event = Event.from(request.event)
        val platform = Platform.from(request.platform)
        val entryPoint = EntryPoint.from(request.entryPoint)
        val installationId = UUID.fromString(request.installationId).toString()
        require(APP_VERSION_REGEX.matches(request.appVersion))

        val stopId = request.stopId?.let { Stop.from(it).value }
        require((event == Event.STOP_SELECTED) == (stopId != null))

        meterRegistry
            .counter(
                "hyuabot.watch.events",
                "event",
                event.value,
                "platform",
                platform.value,
                "entry_point",
                entryPoint.value,
                "stop_id",
                stopId ?: NONE,
            ).increment()

        val key = activeInstallationKey(platform.value, LocalDate.now(watchAnalyticsClock))
        redisTemplate.opsForHyperLogLog().add(key, "${platform.value}:$installationId")
        redisTemplate.expire(key, ACTIVE_KEY_TTL)
    }

    internal fun activeInstallationKeys(
        platform: String,
        today: LocalDate = LocalDate.now(watchAnalyticsClock),
    ): List<String> {
        val platforms = if (platform == ALL_PLATFORMS) Platform.entries.map { it.value } else listOf(platform)
        return platforms.flatMap { platformValue ->
            (0 until ACTIVE_WINDOW_DAYS).map { dayOffset ->
                activeInstallationKey(platformValue, today.minusDays(dayOffset.toLong()))
            }
        }
    }

    private fun registerActiveInstallationGauge(platform: String) {
        Gauge
            .builder("hyuabot.watch.active.installations") { activeInstallationCounts.getValue(platform) }
            .description("Estimated distinct Watch installations active during the rolling window")
            .tag("platform", platform)
            .tag("window", "${ACTIVE_WINDOW_DAYS}d")
            .register(meterRegistry)
    }

    private fun activeInstallationKey(
        platform: String,
        date: LocalDate,
    ) = "analytics:watch:active:$platform:$date"

    private enum class Event(
        val value: String,
    ) {
        APP_OPEN("watch_app_open"),
        STOP_SELECTED("watch_stop_selected"),
        ;

        companion object {
            fun from(value: String) = entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException()
        }
    }

    private enum class Platform(
        val value: String,
    ) {
        WATCH_OS("watchos"),
        WEAR_OS("wear_os"),
        ;

        companion object {
            fun from(value: String) = entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException()
        }
    }

    private enum class EntryPoint(
        val value: String,
    ) {
        APP("app"),
        TILE("tile"),
        COMPLICATION("complication"),
        ;

        companion object {
            fun from(value: String) = entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException()
        }
    }

    private enum class Stop(
        val value: String,
    ) {
        DORMITORY("dormitory"),
        SHUTTLECOCK("shuttlecock"),
        STATION("station"),
        TERMINAL("terminal"),
        JUNGANG("jungang"),
        ;

        companion object {
            fun from(value: String) = entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException()
        }
    }

    companion object {
        private const val ACTIVE_WINDOW_DAYS = 28
        private const val ALL_PLATFORMS = "all"
        private const val NONE = "none"
        private const val ACTIVE_INSTALLATION_REFRESH_INTERVAL_MS = 300_000L
        private val ACTIVE_KEY_TTL = Duration.ofDays(35)
        private val APP_VERSION_REGEX = Regex("[0-9A-Za-z][0-9A-Za-z.+_-]{0,31}")
    }
}
