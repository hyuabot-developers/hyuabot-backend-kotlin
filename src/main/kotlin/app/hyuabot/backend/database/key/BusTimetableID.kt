package app.hyuabot.backend.database.key

import java.io.Serializable
import java.time.LocalTime

data class BusTimetableID(
    val routeID: Int = 0,
    val startStopID: Int = 0,
    val weekday: String = "weekdays",
    val departureTime: LocalTime = LocalTime.now(),
) : Serializable
