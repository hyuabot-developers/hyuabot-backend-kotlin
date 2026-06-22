package app.hyuabot.backend.liveactivity.domain

import java.time.Instant

data class ShuttleLiveActivityRegisterRequest(
    val key: String,
    val pushToken: String,
    val apnsEnvironment: String,
    val alarmKind: String,
    val titleText: String,
    val statusText: String,
    val dynamicIslandStatusText: String,
    val currentStopName: String,
    val nextStopName: String,
    val checkpointWaitingFormat: String,
    val checkpointApproachingFormat: String,
    val checkpointDepartedFormat: String,
    val progressSegments: List<Int>,
    val createdAt: Instant,
    val expiresAt: Instant,
    val checkpoints: List<ShuttleLiveActivityCheckpointRequest>,
)
