package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.BusTimetable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalTime

interface BusTimetableRepository : JpaRepository<BusTimetable, Int> {
    fun findByRouteIDAndStartStopID(
        routeID: Int,
        startStopID: Int,
    ): List<BusTimetable>

    fun findByRouteIDAndStartStopIDAndWeekday(
        routeID: Int,
        startStopID: Int,
        weekday: String,
    ): List<BusTimetable>

    fun findByRouteIDAndStartStopIDAndWeekdayAndDepartureTime(
        routeID: Int,
        startStopID: Int,
        weekday: String,
        departureTime: LocalTime,
    ): BusTimetable?
}
