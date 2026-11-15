package app.hyuabot.backend.holiday

import app.hyuabot.backend.holiday.service.PublicHolidayService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@DgsComponent
class PublicHolidayDataFetcher(
    private val publicHolidayService: PublicHolidayService,
) {
    /** Route buses run the Sunday timetable on these dates, matching BusRealtimeService.resolveWeekday. */
    @DgsQuery
    fun publicHolidays(
        @InputArgument start: LocalDate,
        @InputArgument end: LocalDate,
    ): List<LocalDate> {
        require(!start.isAfter(end)) { "Start date must be before or equal to end date" }
        require(ChronoUnit.DAYS.between(start, end) < MAX_RANGE_DAYS) { "Date range must not exceed $MAX_RANGE_DAYS days" }
        return generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .filter { publicHolidayService.findPublicHoliday(it) != null }
            .toList()
    }

    companion object {
        const val MAX_RANGE_DAYS = 62L
    }
}
