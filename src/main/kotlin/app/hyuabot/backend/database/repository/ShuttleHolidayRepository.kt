package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.ShuttleHoliday
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ShuttleHolidayRepository : JpaRepository<ShuttleHoliday, Int> {
    fun findByDateAndCalendarType(
        date: LocalDate,
        calendarType: String,
    ): ShuttleHoliday?
}
