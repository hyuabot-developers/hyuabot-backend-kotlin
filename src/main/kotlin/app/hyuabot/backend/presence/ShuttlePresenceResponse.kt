package app.hyuabot.backend.presence

import java.time.Instant

data class ShuttlePresenceResponse(
    val viewerCount: Long?,
    val visible: Boolean,
    val updatedAt: Instant,
    val activeWindowSeconds: Long,
)
