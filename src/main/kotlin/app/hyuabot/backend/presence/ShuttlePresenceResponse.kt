package app.hyuabot.backend.presence

import java.time.Instant

data class ShuttlePresenceResponse(
    val viewerCount: Long?,
    val visible: Boolean,
    val updatedAt: Instant,
    val activeWindowSeconds: Long,
)

data class ShuttlePresenceCountsResponse(
    val stops: List<StopViewerCount>,
    val updatedAt: Instant,
) {
    data class StopViewerCount(
        val stopId: String,
        val viewerCount: Long?,
        val visible: Boolean,
    )
}
