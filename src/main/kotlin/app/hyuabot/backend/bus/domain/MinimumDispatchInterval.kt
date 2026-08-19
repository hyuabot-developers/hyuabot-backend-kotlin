package app.hyuabot.backend.bus.domain

data class MinimumDispatchInterval(
    val weekday: String,
    val minutes: Int,
)
