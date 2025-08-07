package app.hyuabot.backend.shuttle.domain

data class CreateShuttleStopRequest(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)
