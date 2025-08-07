package app.hyuabot.backend.shuttle.domain

data class ShuttleTimetableRequest(
    val periodType: String,
    val weekday: Boolean,
    val departureTime: String,
)
