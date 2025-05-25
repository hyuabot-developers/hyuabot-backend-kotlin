package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.ShuttleTimetableView
import app.hyuabot.backend.database.key.ShuttleTimetableViewID
import org.springframework.data.jpa.repository.JpaRepository

interface ShuttleTimetableViewRepository : JpaRepository<ShuttleTimetableView, ShuttleTimetableViewID> {
    fun findByPeriodType(periodType: String): List<ShuttleTimetableView>

    fun findByPeriodTypeAndWeekday(
        periodType: String,
        isWeekdays: Boolean,
    ): List<ShuttleTimetableView>

    fun findByPeriodTypeAndRouteName(
        periodType: String,
        routeName: String,
    ): List<ShuttleTimetableView>

    fun findByPeriodTypeAndRouteTag(
        periodType: String,
        routeTag: String,
    ): List<ShuttleTimetableView>

    fun findByPeriodTypeAndStopName(
        periodType: String,
        stopName: String,
    ): List<ShuttleTimetableView>

    fun findByPeriodTypeAndRouteNameAndWeekday(
        periodType: String,
        routeName: String,
        isWeekdays: Boolean,
    ): List<ShuttleTimetableView>

    fun findByPeriodTypeAndRouteTagAndWeekday(
        periodType: String,
        routeTag: String,
        isWeekdays: Boolean,
    ): List<ShuttleTimetableView>

    fun findByPeriodTypeAndStopNameAndWeekday(
        periodType: String,
        stopName: String,
        isWeekdays: Boolean,
    ): List<ShuttleTimetableView>
}
