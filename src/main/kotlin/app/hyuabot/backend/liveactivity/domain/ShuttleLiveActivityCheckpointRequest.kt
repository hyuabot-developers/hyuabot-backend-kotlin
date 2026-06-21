package app.hyuabot.backend.liveactivity.domain

import java.time.Instant

data class ShuttleLiveActivityCheckpointRequest(
    val name: String,
    val time: Instant,
)
