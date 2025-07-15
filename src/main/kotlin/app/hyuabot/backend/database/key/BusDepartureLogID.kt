package app.hyuabot.backend.database.key

import java.io.Serializable
import java.time.LocalDate
import java.time.LocalTime

data class BusDepartureLogID(
    val routeID: Int = 0,
    val stopID: Int = 0,
    val departureDate: LocalDate = LocalDate.now(),
    val departureTime: LocalTime = LocalTime.now(),
) : Serializable
