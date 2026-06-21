package app.hyuabot.backend.liveactivity.service

import app.hyuabot.backend.liveactivity.domain.ShuttleLiveActivityRegisterRequest
import app.hyuabot.backend.liveactivity.domain.ShuttleLiveActivityRegisterResponse
import app.hyuabot.backend.liveactivity.domain.toState
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ScheduledFuture

@Service
class ShuttleLiveActivityService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val taskScheduler: TaskScheduler,
    private val apnsLiveActivityService: ApnsLiveActivityService,
) {
    private val scheduledTasks = mutableMapOf<String, MutableList<ScheduledFuture<*>>>()

    fun register(request: ShuttleLiveActivityRegisterRequest): ShuttleLiveActivityRegisterResponse {
        unregister(request.key)
        val expiresAt = request.expiresAt
        val ttl = Duration.between(Instant.now(), expiresAt.plus(Duration.ofMinutes(10))).coerceAtLeast(Duration.ofMinutes(1))
        redisTemplate.opsForValue().set(redisKey(request.key), request.pushToken, ttl)

        val pushDates = pushDates(request)
        scheduledTasks[request.key] =
            pushDates
                .map { date ->
                    taskScheduler.schedule(
                        {
                            apnsLiveActivityService.sendUpdate(
                                token = request.pushToken,
                                apnsEnvironment = request.apnsEnvironment,
                                state = request.toState(Instant.now()),
                                staleDate = expiresAt,
                            )
                        },
                        date,
                    )
                }.toMutableList()

        apnsLiveActivityService.sendUpdate(request.pushToken, request.apnsEnvironment, request.toState(Instant.now()), expiresAt)
        return ShuttleLiveActivityRegisterResponse(request.key, pushDates.size)
    }

    fun unregister(key: String) {
        scheduledTasks.remove(key)?.forEach { it.cancel(false) }
        redisTemplate.delete(redisKey(key))
    }

    fun end(request: ShuttleLiveActivityRegisterRequest) {
        unregister(request.key)
        apnsLiveActivityService.sendEnd(request.pushToken, request.apnsEnvironment, request.toState(Instant.now()))
    }

    private fun pushDates(request: ShuttleLiveActivityRegisterRequest): List<Instant> {
        val now = Instant.now()
        val checkpointDates =
            request.checkpoints
                .flatMap { listOf(it.time.minusSeconds(60), it.time, it.time.plusSeconds(1)) }
        val periodicDates =
            generateSequence(now.plusSeconds(15)) { it.plusSeconds(15) }
                .takeWhile { it <= request.expiresAt.plusSeconds(60) }
                .toList()
        return (checkpointDates + periodicDates + request.expiresAt)
            .filter { it > now }
            .distinct()
            .sorted()
            .take(96)
    }

    private fun redisKey(key: String) = "live-activity:shuttle:$key"
}
