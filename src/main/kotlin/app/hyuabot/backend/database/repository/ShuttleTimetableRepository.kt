package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.ShuttleTimetable
import org.springframework.data.jpa.repository.JpaRepository

interface ShuttleTimetableRepository : JpaRepository<ShuttleTimetable, Int> {
    fun findByPeriodType(periodType: String): List<ShuttleTimetable>

    fun findByPeriodTypeAndWeekday(
        periodType: String,
        isWeekdays: Boolean,
    ): List<ShuttleTimetable>

    fun findByPeriodTypeAndRouteName(
        periodType: String,
        routeName: String,
    ): List<ShuttleTimetable>

    fun findByPeriodTypeAndRouteNameAndWeekday(
        periodType: String,
        routeName: String,
        isWeekdays: Boolean,
    ): List<ShuttleTimetable>
}
