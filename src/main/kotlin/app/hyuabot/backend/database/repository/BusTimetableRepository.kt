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
}
