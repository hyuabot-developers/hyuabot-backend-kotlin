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

    fun findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsIn(
        periods: List<String>,
        stops: List<String>,
        weekdays: List<Boolean>,
    ): List<ShuttleTimetableView>

    fun findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteNameIsIn(
        periods: List<String>,
        stops: List<String>,
        weekdays: List<Boolean>,
        routes: List<String>,
    ): List<ShuttleTimetableView>

    fun findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteTagIsIn(
        periods: List<String>,
        stops: List<String>,
        weekdays: List<Boolean>,
        tags: List<String>,
    ): List<ShuttleTimetableView>

    fun findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndDestinationGroupIsIn(
        periods: List<String>,
        stops: List<String>,
        weekdays: List<Boolean>,
        groups: List<String>,
    ): List<ShuttleTimetableView>

    fun findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteNameIsInAndRouteTagIsIn(
        periods: List<String>,
        stops: List<String>,
        weekdays: List<Boolean>,
        routes: List<String>,
        tags: List<String>,
    ): List<ShuttleTimetableView>

    fun findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteNameIsInAndDestinationGroupIsIn(
        periods: List<String>,
        stops: List<String>,
        weekdays: List<Boolean>,
        routes: List<String>,
        groups: List<String>,
    ): List<ShuttleTimetableView>

    fun findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteTagIsInAndDestinationGroupIsIn(
        periods: List<String>,
        stops: List<String>,
        weekdays: List<Boolean>,
        tags: List<String>,
        groups: List<String>,
    ): List<ShuttleTimetableView>

    fun findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteNameIsInAndRouteTagIsInAndDestinationGroupIsIn(
        periods: List<String>,
        stops: List<String>,
        weekdays: List<Boolean>,
        routes: List<String>,
        tags: List<String>,
        groups: List<String>,
    ): List<ShuttleTimetableView>

    fun findBySeqIn(seq: List<Int>): List<ShuttleTimetableView>
}
