package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.BusTimetable
import app.hyuabot.backend.holiday.audit.BusHolidayCoverageGap
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalTime

interface BusTimetableRepository : JpaRepository<BusTimetable, Int> {
    @Query(
        """
        SELECT new app.hyuabot.backend.holiday.audit.BusHolidayCoverageGap(t.routeID, t.startStopID)
        FROM bus_timetable t
        GROUP BY t.routeID, t.startStopID
        HAVING SUM(CASE WHEN t.weekday = 'weekdays' THEN 1 ELSE 0 END) > 0
           AND SUM(CASE WHEN t.weekday = 'sunday' THEN 1 ELSE 0 END) = 0
        """,
    )
    fun findHolidayCoverageGaps(): List<BusHolidayCoverageGap>

    fun findByRouteIDAndStartStopIDAndWeekdayAndDepartureTime(
        routeID: Int,
        startStopID: Int,
        weekday: String,
        departureTime: LocalTime,
    ): BusTimetable?

    fun findByRouteIDAndStartStopIDAndWeekdayIsIn(
        routeID: Int,
        startStopID: Int,
        weekday: List<String>,
        sort: Sort,
    ): List<BusTimetable>

    fun findByRouteIDAndStartStopID(
        routeID: Int,
        startStopID: Int,
        sort: Sort,
    ): List<BusTimetable>

    fun findByRouteIDAndWeekdayIsIn(
        routeID: Int,
        weekday: List<String>,
        sort: Sort,
    ): List<BusTimetable>

    fun findByStartStopIDAndWeekdayIsIn(
        startStopID: Int,
        weekday: List<String>,
        sort: Sort,
    ): List<BusTimetable>

    fun findByRouteID(
        routeID: Int,
        sort: Sort,
    ): List<BusTimetable>

    fun findByStartStopID(
        startStopID: Int,
        sort: Sort,
    ): List<BusTimetable>

    fun findByWeekdayIsIn(
        weekday: List<String>,
        sort: Sort,
    ): List<BusTimetable>

    fun findByRouteIDInAndStartStopIDInAndDepartureTimeAfter(
        routeIDs: List<Int>,
        startStopIDs: List<Int>,
        after: LocalTime,
        sort: Sort,
    ): List<BusTimetable>

    fun findByRouteIDInAndStartStopIDInAndWeekdayAndDepartureTimeAfter(
        routeIDs: List<Int>,
        startStopIDs: List<Int>,
        weekday: String,
        after: LocalTime,
        sort: Sort,
    ): List<BusTimetable>

    fun findByRouteIDInAndStartStopIDInAndWeekdayAndDepartureTimeBefore(
        routeIDs: List<Int>,
        startStopIDs: List<Int>,
        weekday: String,
        before: LocalTime,
        sort: Sort,
    ): List<BusTimetable>
}
