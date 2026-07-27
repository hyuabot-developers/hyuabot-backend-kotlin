package app.hyuabot.backend.presence

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

@Service
class AdminShuttlePresenceService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val shuttleDemandWindowService: ShuttleDemandWindowService,
    private val watchAnalyticsClock: Clock,
) {
    fun getViewerCounts(): AdminShuttlePresenceResponse {
        val now = Instant.now(watchAnalyticsClock)
        val stops =
            ShuttleDemandWindowService.STOP_NAMES.map { stopId ->
                val window = shuttleDemandWindowService.demandWindow(stopId, now)
                val count =
                    if (window == null) {
                        0L
                    } else {
                        redisTemplate
                            .opsForZSet()
                            .count("presence:shuttle:$stopId", window.startEpoch.toDouble(), Double.POSITIVE_INFINITY) ?: 0L
                    }
                AdminShuttlePresenceResponse.StopViewerCount(stopId, count)
            }
        return AdminShuttlePresenceResponse(
            stops = stops,
            updatedAt = now,
            activeWindowSeconds = 0L,
        )
    }
}
