package app.hyuabot.backend.shuttle.domain

data class UpdateShuttleRouteStopRequest(
    val order: Int,
    val cumulativeTime: String,
)
