package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.ShuttleHoliday
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface ShuttleHolidayRepository : JpaRepository<ShuttleHoliday, Int> {
    fun findByDateAndCalendarType(
        date: LocalDate,
        calendarType: String,
    ): ShuttleHoliday?

    fun findBySeqNotAndDateAndCalendarType(
        seq: Int,
        date: LocalDate,
        calendarType: String,
    ): ShuttleHoliday?

    @Query(
        """
        SELECT h FROM shuttle_holiday h
        WHERE (h.date = :solarDate AND h.calendarType = 'solar')
           OR (h.date = :lunarDate AND h.calendarType = 'lunar')
        """,
    )
    fun findBySolarDateOrLunarDate(
        solarDate: LocalDate,
        lunarDate: LocalDate,
    ): ShuttleHoliday?
}
