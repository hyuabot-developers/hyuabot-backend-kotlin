package app.hyuabot.backend.shuttle.domain

import app.hyuabot.backend.database.entity.ShuttleHoliday
import java.time.LocalDate

data class ShuttleHolidayOccurrence(
    val date: LocalDate,
    val holiday: ShuttleHoliday,
)
