package app.hyuabot.backend.database.key

import java.io.Serializable
import java.time.LocalDate
import java.time.LocalTime

data class BusDepartureLogID(
    val routeID: Int,
    val stopID: Int,
    val departureDate: LocalDate,
    val departureTime: LocalTime,
) : Serializable
