package app.hyuabot.backend.presence

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

@Service
class AdminShuttlePresenceService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val watchAnalyticsClock: Clock,
) {
    fun getViewerCounts(): AdminShuttlePresenceResponse {
        val now = Instant.now(watchAnalyticsClock)
        val minScore = (now.epochSecond - ACTIVE_WINDOW_SECONDS).toDouble()
        val maxScore = Double.POSITIVE_INFINITY

        val stops =
            SHUTTLE_STOP_IDS.map { stopId ->
                val count =
                    redisTemplate.opsForZSet().count("presence:shuttle:$stopId", minScore, maxScore) ?: 0L
                AdminShuttlePresenceResponse.StopViewerCount(stopId, count)
            }

        return AdminShuttlePresenceResponse(
            stops = stops,
            updatedAt = now,
            activeWindowSeconds = ACTIVE_WINDOW_SECONDS,
        )
    }

    companion object {
        internal const val ACTIVE_WINDOW_SECONDS = 75L
        internal val SHUTTLE_STOP_IDS =
            listOf(
                "dormitory_o",
                "shuttlecock_o",
                "station",
                "terminal",
                "jungang_stn",
                "shuttlecock_i",
            )
    }
}
