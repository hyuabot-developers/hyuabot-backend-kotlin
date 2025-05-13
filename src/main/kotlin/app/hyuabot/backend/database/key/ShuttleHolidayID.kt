package app.hyuabot.backend.database.key

import java.time.LocalDate

data class ShuttleHolidayID(
    val date: LocalDate,
    val type: String,
    val calendarType: String,
)
