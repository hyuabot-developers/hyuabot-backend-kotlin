package app.hyuabot.backend.shuttle.domain

data class CreateShuttleRouteRequest(
    val name: String,
    val descriptionKorean: String,
    val descriptionEnglish: String,
    val tag: String,
    val startStopID: String,
    val endStopID: String,
)
