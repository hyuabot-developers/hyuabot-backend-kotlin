package app.hyuabot.backend.liveactivity.domain

import java.time.Instant

data class ShuttleLiveActivityState(
    val titleText: String,
    val statusText: String,
    val dynamicIslandStatusText: String,
    val currentStopName: String,
    val nextStopName: String,
    val checkpointStopNames: List<String>,
    val checkpointTimes: List<Double>,
    val checkpointWaitingFormat: String,
    val checkpointApproachingFormat: String,
    val checkpointDepartedFormat: String,
    val progress: Int,
    val progressSegments: List<Int>,
)

fun ShuttleLiveActivityRegisterRequest.toState(now: Instant): ShuttleLiveActivityState {
    val times = checkpoints.map { it.time }
    val progress =
        if (times.size >= 2) {
            checkpointProgress(times, now)
        } else {
            fallbackProgress(createdAt, expiresAt, now)
        }
    return ShuttleLiveActivityState(
        titleText = titleText,
        statusText = statusText,
        dynamicIslandStatusText = dynamicIslandStatusText,
        currentStopName = currentStopName,
        nextStopName = nextStopName,
        checkpointStopNames = checkpoints.map { it.name },
        checkpointTimes = times.map { it.toAppleReferenceSeconds() },
        checkpointWaitingFormat = checkpointWaitingFormat,
        checkpointApproachingFormat = checkpointApproachingFormat,
        checkpointDepartedFormat = checkpointDepartedFormat,
        progress = progress,
        progressSegments = progressSegments.ifEmpty { listOf(100) },
    )
}

private fun checkpointProgress(
    times: List<Instant>,
    now: Instant,
): Int {
    val start = times.first()
    val end = times.last()
    val total = end.epochMillis - start.epochMillis
    if (total <= 0) return 0
    val elapsed = (now.epochMillis - start.epochMillis).coerceIn(0, total)
    return ((elapsed * 100) / total).toInt().coerceIn(0, 100)
}

private fun fallbackProgress(
    start: Instant,
    end: Instant,
    now: Instant,
): Int {
    val total = end.epochMillis - start.epochMillis
    if (total <= 0) return 100
    val elapsed = (now.epochMillis - start.epochMillis).coerceIn(0, total)
    return ((elapsed * 100) / total).toInt().coerceIn(0, 100)
}

private val Instant.epochMillis: Long
    get() = toEpochMilli()

private fun Instant.toAppleReferenceSeconds(): Double = epochSecond - 978_307_200.0 + nano / 1_000_000_000.0
