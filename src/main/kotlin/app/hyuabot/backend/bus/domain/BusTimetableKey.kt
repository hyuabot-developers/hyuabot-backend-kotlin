package app.hyuabot.backend.bus.domain

import java.time.LocalTime

data class BusTimetableKey(
    val routeID: Int,
    val startStopID: Int,
    val weekdays: List<String>?,
    val after: LocalTime?,
)
