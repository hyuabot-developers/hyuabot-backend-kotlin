package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.BusTimetable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalTime

interface BusTimetableRepository : JpaRepository<BusTimetable, Int> {
    fun findByRouteIDAndStartStopIDAndWeekdayAndDepartureTime(
        routeID: Int,
        startStopID: Int,
        weekday: String,
        departureTime: LocalTime,
    ): BusTimetable?

    fun findByRouteIDAndStartStopIDAndWeekday(
        routeID: Int,
        startStopID: Int,
        weekday: String,
        sort: Sort,
    ): List<BusTimetable>

    fun findByRouteIDAndStartStopID(
        routeID: Int,
        startStopID: Int,
        sort: Sort,
    ): List<BusTimetable>

    fun findByRouteIDAndWeekday(
        routeID: Int,
        weekday: String,
        sort: Sort,
    ): List<BusTimetable>

    fun findByStartStopIDAndWeekday(
        startStopID: Int,
        weekday: String,
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

    fun findByWeekday(
        weekday: String,
        sort: Sort,
    ): List<BusTimetable>
}
