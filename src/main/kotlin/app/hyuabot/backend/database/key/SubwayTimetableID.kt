package app.hyuabot.backend.database.key

import java.io.Serializable
import java.time.LocalTime

data class SubwayTimetableID(
    val stationID: String,
    val heading: String,
    val weekday: String,
    val departureTime: LocalTime,
) : Serializable
