package app.hyuabot.backend.database.key

import java.time.LocalDate

data class ShuttleHolidayID(
    val date: LocalDate = LocalDate.MIN,
    val type: String = "",
    val calendarType: String = "",
)
