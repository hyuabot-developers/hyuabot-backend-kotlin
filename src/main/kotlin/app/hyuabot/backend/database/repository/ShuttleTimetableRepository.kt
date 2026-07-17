package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.ShuttleTimetable
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime

interface ShuttleTimetableRepository : JpaRepository<ShuttleTimetable, Int> {
    fun findByRouteName(routeName: String): List<ShuttleTimetable>

    fun findByRouteNameIn(routeNames: List<String>): List<ShuttleTimetable>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT timetable FROM shuttle_timetable timetable WHERE timetable.routeName IN :routeNames")
    fun findByRouteNameInForUpdate(routeNames: List<String>): List<ShuttleTimetable>

    @Transactional
    fun deleteAllByRouteNameIn(routeNames: List<String>)

    fun findByPeriodType(periodType: String): List<ShuttleTimetable>

    fun existsByPeriodTypeAndWeekday(
        periodType: String,
        weekday: Boolean,
    ): Boolean

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
