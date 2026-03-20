package app.hyuabot.backend.bus.domain

data class BusTimetableKey(
    val routeID: Int,
    val startStopID: Int,
    val weekdays: List<String>?,
)
