package app.hyuabot.backend.database.key

import java.io.Serializable
import java.time.LocalTime

data class BusTimetableID(
    val routeID: Int,
    val startStopID: Int,
    val weekday: String,
    val departureTime: LocalTime,
) : Serializable
