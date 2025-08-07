package app.hyuabot.backend.shuttle.domain

data class UpdateShuttleRouteRequest(
    val descriptionKorean: String,
    val descriptionEnglish: String,
    val tag: String,
    val startStopID: String,
    val endStopID: String,
)
