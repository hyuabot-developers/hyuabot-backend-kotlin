package app.hyuabot.backend.liveactivity

import app.hyuabot.backend.liveactivity.domain.ShuttleLiveActivityCheckpointRequest
import app.hyuabot.backend.liveactivity.domain.ShuttleLiveActivityRegisterRequest
import app.hyuabot.backend.liveactivity.domain.toState
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

class ShuttleLiveActivityStateTest {
    @Test
    @DisplayName("Checkpoint state uses checkpoint progress and Apple reference date seconds")
    fun checkpointState() {
        val request =
            registerRequest(
                checkpoints =
                    listOf(
                        ShuttleLiveActivityCheckpointRequest("Start", Instant.parse("2026-06-21T00:00:00Z")),
                        ShuttleLiveActivityCheckpointRequest("End", Instant.parse("2026-06-21T00:10:00Z")),
                    ),
                progressSegments = listOf(100),
            )

        val state = request.toState(Instant.parse("2026-06-21T00:05:00Z"))

        assertEquals(50, state.progress)
        assertEquals(listOf("Start", "End"), state.checkpointStopNames)
        assertEquals(803_692_800.0, state.checkpointTimes.first())
        assertEquals(listOf(100), state.progressSegments)
    }

    @Test
    @DisplayName("Single checkpoint state uses created and expiration dates for fallback progress")
    fun fallbackState() {
        val request =
            registerRequest(
                checkpoints = listOf(ShuttleLiveActivityCheckpointRequest("Only", Instant.parse("2026-06-21T00:00:00Z"))),
                progressSegments = emptyList(),
            )

        val state = request.toState(Instant.parse("2026-06-21T00:05:00Z"))

        assertEquals(50, state.progress)
        assertEquals(listOf(100), state.progressSegments)
    }

    @Test
    @DisplayName("Invalid durations clamp to completion or zero")
    fun invalidDurations() {
        val fallback =
            registerRequest(
                createdAt = Instant.parse("2026-06-21T00:10:00Z"),
                expiresAt = Instant.parse("2026-06-21T00:00:00Z"),
                checkpoints = emptyList(),
            ).toState(Instant.parse("2026-06-21T00:05:00Z"))
        val checkpoint =
            registerRequest(
                checkpoints =
                    listOf(
                        ShuttleLiveActivityCheckpointRequest("Start", Instant.parse("2026-06-21T00:00:00Z")),
                        ShuttleLiveActivityCheckpointRequest("End", Instant.parse("2026-06-21T00:00:00Z")),
                    ),
            ).toState(Instant.parse("2026-06-21T00:05:00Z"))

        assertEquals(100, fallback.progress)
        assertEquals(0, checkpoint.progress)
    }

    private fun registerRequest(
        createdAt: Instant = Instant.parse("2026-06-21T00:00:00Z"),
        expiresAt: Instant = Instant.parse("2026-06-21T00:10:00Z"),
        checkpoints: List<ShuttleLiveActivityCheckpointRequest>,
        progressSegments: List<Int> = listOf(100),
    ) = ShuttleLiveActivityRegisterRequest(
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
        progressSegments = progressSegments,
        createdAt = createdAt,
        expiresAt = expiresAt,
        checkpoints = checkpoints,
    )
}
