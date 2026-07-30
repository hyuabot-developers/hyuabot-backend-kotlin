package app.hyuabot.backend.presence

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.scripting.support.ResourceScriptSource
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class ShuttlePresenceService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val meterRegistry: MeterRegistry,
    private val watchAnalyticsClock: Clock,
    private val shuttleDemandWindowService: ShuttleDemandWindowService,
) {
    private val heartbeatScript =
        DefaultRedisScript<Long>().apply {
            setScriptSource(ResourceScriptSource(ClassPathResource("scripts/shuttle-presence.lua")))
            resultType = Long::class.java
        }

    fun heartbeat(request: ShuttlePresenceRequest): ShuttlePresenceResponse {
        val stop = Stop.from(request.stopId)
        val platform = Platform.from(request.platform)
        val sessionId = UUID.fromString(request.sessionId).toString()
        require(APP_VERSION_REGEX.matches(request.appVersion))

        val now = Instant.now(watchAnalyticsClock)

        meterRegistry
            .counter(
                "hyuabot.shuttle.presence.heartbeats",
                "platform",
                platform.value,
                "stop_id",
                stop.value,
            ).increment()

        val window = shuttleDemandWindowService.demandWindow(stop.value, now) ?: return responseFor(0L, now, 0L)

        val nowEpoch = now.epochSecond
        val stopKey = "presence:shuttle:${stop.value}"
        val sessionKey = "presence:shuttle:session:$sessionId"
        val rateKey = "presence:shuttle:rate:$sessionId"
        val count =
            redisTemplate.execute(
                heartbeatScript,
                listOf(stopKey, sessionKey, rateKey),
                window.startEpoch.toString(),
                nowEpoch.toString(),
                sessionId,
                stopKey,
                window.keyTtlSeconds.toString(),
                window.keyTtlSeconds.toString(),
                MIN_HEARTBEAT_INTERVAL_SECONDS.toString(),
            ) ?: 0L

        return responseFor(count, now, nowEpoch - window.startEpoch)
    }

    fun getViewerCounts(): ShuttlePresenceCountsResponse {
        val now = Instant.now(watchAnalyticsClock)
        val nowEpoch = now.epochSecond
        val counts =
            Stop.entries.map { stop ->
                val window = shuttleDemandWindowService.demandWindow(stop.value, now)
                val count =
                    if (window == null) {
                        0L
                    } else {
                        redisTemplate
                            .opsForZSet()
                            .count("presence:shuttle:${stop.value}", window.startEpoch.toDouble(), Double.POSITIVE_INFINITY) ?: 0L
                    }
                val activeWindowSeconds = window?.let { nowEpoch - it.startEpoch } ?: 0L
                val response = responseFor(count, now, activeWindowSeconds)
                ShuttlePresenceCountsResponse.StopViewerCount(stop.value, response.viewerCount, response.visible)
            }
        return ShuttlePresenceCountsResponse(stops = counts, updatedAt = now)
    }

    internal fun responseFor(
        count: Long,
        now: Instant,
        activeWindowSeconds: Long,
    ): ShuttlePresenceResponse {
        val visible = count >= MINIMUM_VISIBLE_COUNT
        return ShuttlePresenceResponse(
            viewerCount = count.takeIf { visible },
            visible = visible,
            updatedAt = now,
            activeWindowSeconds = activeWindowSeconds,
        )
    }

    private enum class Platform(
        val value: String,
    ) {
        ANDROID("android"),
        IOS("ios"),
        ;

        companion object {
            fun from(value: String) = entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException()
        }
    }

    private enum class Stop(
        val value: String,
    ) {
        DORMITORY("dormitory_o"),
        SHUTTLECOCK_OUT("shuttlecock_o"),
        STATION("station"),
        TERMINAL("terminal"),
        JUNGANG("jungang_stn"),
        SHUTTLECOCK_IN("shuttlecock_i"),
        ;

        companion object {
            fun from(value: String) = entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException()
        }
    }

    companion object {
        private const val MIN_HEARTBEAT_INTERVAL_SECONDS = 5L
        private const val MINIMUM_VISIBLE_COUNT = 3L
        private val APP_VERSION_REGEX = Regex("[0-9A-Za-z][0-9A-Za-z.+_-]{0,31}")
    }
}
