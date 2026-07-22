package app.hyuabot.backend.presence

data class ShuttlePresenceRequest(
    val stopId: String,
    val sessionId: String,
    val platform: String,
    val appVersion: String,
)
