package app.hyuabot.backend.database.key

import java.io.Serializable
import java.time.LocalDate

data class SubwayTrainDelayID(
    val runDate: LocalDate = LocalDate.MIN,
    val trainNumber: String = "",
) : Serializable
