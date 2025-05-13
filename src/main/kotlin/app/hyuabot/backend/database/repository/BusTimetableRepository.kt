package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.BusTimetable
import app.hyuabot.backend.database.key.BusTimetableID
import org.springframework.data.jpa.repository.JpaRepository

interface BusTimetableRepository : JpaRepository<BusTimetable, BusTimetableID> {
    fun findByRouteIDAndStartStopID(
        routeID: Int,
        startStopID: Int,
    ): List<BusTimetable>

    fun findByRouteIDAndStartStopIDAndWeekday(
        routeID: Int,
        startStopID: Int,
        weekday: String,
    ): List<BusTimetable>
}
