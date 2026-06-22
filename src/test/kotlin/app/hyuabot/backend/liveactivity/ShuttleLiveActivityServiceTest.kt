package app.hyuabot.backend.liveactivity

import app.hyuabot.backend.liveactivity.domain.ShuttleLiveActivityCheckpointRequest
import app.hyuabot.backend.liveactivity.domain.ShuttleLiveActivityRegisterRequest
import app.hyuabot.backend.liveactivity.service.ApnsLiveActivityService
import app.hyuabot.backend.liveactivity.service.ShuttleLiveActivityService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.scheduling.TaskScheduler
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ScheduledFuture
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class ShuttleLiveActivityServiceTest {
    @Test
    @DisplayName("Register stores token, schedules pushes, and sends initial update")
    fun register() {
        val valueOperations = mock<ValueOperations<String, String>>()
        val redisTemplate = mock<RedisTemplate<String, String>>()
        val scheduler = mock<TaskScheduler>()
        val apns = mock<ApnsLiveActivityService>()
        val future = mock<ScheduledFuture<*>>()
        whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
        whenever(scheduler.schedule(any<Runnable>(), any<Instant>())).thenReturn(future)
        val service = ShuttleLiveActivityService(redisTemplate, scheduler, apns)
        val request = registerRequest()

        val response = service.register(request)
        val scheduledRunnable = argumentCaptor<Runnable>()
        verify(scheduler, atLeastOnce()).schedule(scheduledRunnable.capture(), any<Instant>())
        scheduledRunnable.firstValue.run()

        assertEquals("key", response.key)
        assertEquals(10, response.scheduledPushCount)
        verify(valueOperations).set(
            eq("live-activity:shuttle:key"),
            eq("token"),
            any<Duration>(),
        )
        verify(apns, org.mockito.kotlin.times(2)).sendUpdate(eq("token"), eq("development"), any(), eq(request.expiresAt))
    }

    @Test
    @DisplayName("Unregister cancels scheduled pushes and removes Redis key")
    fun unregister() {
        val redisTemplate = mock<RedisTemplate<String, String>>()
        val scheduler = mock<TaskScheduler>()
        val apns = mock<ApnsLiveActivityService>()
        val future =
            mock<ScheduledFuture<*>> {
                on { cancel(false) } doReturn true
            }
        whenever(scheduler.schedule(any<Runnable>(), any<Instant>())).thenReturn(future)
        whenever(redisTemplate.opsForValue()).thenReturn(mock())
        val service = ShuttleLiveActivityService(redisTemplate, scheduler, apns)

        service.register(registerRequest())
        service.unregister("key")

        verify(future, atLeastOnce()).cancel(false)
        verify(redisTemplate, atLeastOnce()).delete("live-activity:shuttle:key")
    }

    @Test
    @DisplayName("End unregisters and sends final APNs event")
    fun end() {
        val redisTemplate = mock<RedisTemplate<String, String>>()
        val apns = mock<ApnsLiveActivityService>()
        val service =
            ShuttleLiveActivityService(
                redisTemplate = redisTemplate,
                taskScheduler = mock(),
                apnsLiveActivityService = apns,
            )
        val request = registerRequest()

        service.end(request)

        verify(redisTemplate).delete("live-activity:shuttle:key")
        verify(apns).sendEnd(eq("token"), eq("development"), any())
    }

    private fun registerRequest() =
        ShuttleLiveActivityRegisterRequest(
            key = "key",
            pushToken = "token",
            apnsEnvironment = "development",
            alarmKind = "boarding",
            titleText = "title",
            statusText = "status",
            dynamicIslandStatusText = "island",
            currentStopName = "current",
            nextStopName = "next",
            checkpointWaitingFormat = "%s waiting",
            checkpointApproachingFormat = "%s approaching",
            checkpointDepartedFormat = "%s departed",
            progressSegments = listOf(50, 50),
            createdAt = Instant.now().minusSeconds(60),
            expiresAt = Instant.now().plusSeconds(30),
            checkpoints =
                listOf(
                    ShuttleLiveActivityCheckpointRequest("current", Instant.now().plusSeconds(10)),
                    ShuttleLiveActivityCheckpointRequest("next", Instant.now().plusSeconds(20)),
                ),
        )
}
