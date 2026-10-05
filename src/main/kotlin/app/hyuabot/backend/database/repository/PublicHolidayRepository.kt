package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.PublicHoliday
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface PublicHolidayRepository : JpaRepository<PublicHoliday, Int> {
    fun findByDateAndCalendarType(
        date: LocalDate,
        calendarType: String,
    ): PublicHoliday?

    fun findBySeqNotAndDateAndCalendarType(
        seq: Int,
        date: LocalDate,
        calendarType: String,
    ): PublicHoliday?

    @Query(
        """
        SELECT h FROM public_holiday h
        WHERE (h.date = :solarDate AND h.calendarType = 'solar')
           OR (h.date = :lunarDate AND h.calendarType = 'lunar')
        """,
    )
    fun findBySolarDateOrLunarDate(
        solarDate: LocalDate,
        lunarDate: LocalDate,
    ): PublicHoliday?

    @Query(
        value = """
            SELECT seq, holiday_date, holiday_name, calendar_type
            FROM public_holiday
            WHERE source = :source
              AND calendar_type = :calendarType
              AND holiday_date BETWEEN :start AND :end
            ORDER BY holiday_date
        """,
        nativeQuery = true,
    )
    fun findOfficialHolidaysBetween(
        source: String,
        calendarType: String,
        start: LocalDate,
        end: LocalDate,
    ): List<PublicHoliday>
}
