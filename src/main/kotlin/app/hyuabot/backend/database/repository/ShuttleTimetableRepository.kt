package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.ShuttleTimetable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalTime

interface ShuttleTimetableRepository : JpaRepository<ShuttleTimetable, Int> {
    fun findByRouteName(routeName: String): List<ShuttleTimetable>

    fun findByPeriodType(periodType: String): List<ShuttleTimetable>

    fun findByRouteNameAndWeekday(
        routeName: String,
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

    fun findByRouteNameAndPeriodTypeAndWeekdayAndDepartureTime(
        routeName: String,
        periodType: String,
        isWeekdays: Boolean,
        departureTime: LocalTime,
    ): ShuttleTimetable?

    fun findByRouteNameAndSeq(
        routeName: String,
        seq: Int,
    ): ShuttleTimetable?
}
