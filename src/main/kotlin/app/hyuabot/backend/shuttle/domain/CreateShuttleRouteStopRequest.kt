package app.hyuabot.backend.shuttle.domain

data class CreateShuttleRouteStopRequest(
    val stopName: String,
    val order: Int,
    val cumulativeTime: String,
)
