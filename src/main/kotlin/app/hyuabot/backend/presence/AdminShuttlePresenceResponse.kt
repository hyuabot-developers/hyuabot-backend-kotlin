package app.hyuabot.backend.presence

import java.time.Instant

data class AdminShuttlePresenceResponse(
    val stops: List<StopViewerCount>,
    val updatedAt: Instant,
    val activeWindowSeconds: Long,
) {
    data class StopViewerCount(
        val stopId: String,
        val viewerCount: Long,
    )
}
