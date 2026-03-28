package app.hyuabot.backend.bus.service

import app.hyuabot.backend.codegen.types.BusArrival
import app.hyuabot.backend.database.entity.BusRealtime
import app.hyuabot.backend.database.repository.BusRealtimeRepository
import app.hyuabot.backend.database.repository.BusTimetableRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class BusRealtimeService(
    private val realtimeRepository: BusRealtimeRepository,
    private val timetableRepository: BusTimetableRepository,
) {
    internal fun resolveWeekday(date: LocalDate): String =
        when (date.dayOfWeek) {
            DayOfWeek.SATURDAY -> "saturday"
            DayOfWeek.SUNDAY -> "sunday"
            else -> "weekdays"
        }

    fun getBusRealtimeList(): List<BusRealtime> = realtimeRepository.findAll()

    fun getBusRealtimeListByBusStop(
        routeID: Int,
        stopID: Int,
    ): List<BusRealtime> = realtimeRepository.findByRouteIDAndStopID(routeID, stopID)

    fun getBusRealtimeBatch(keys: Set<Pair<Int, Int>>): Map<Pair<Int, Int>, List<BusRealtime>> {
        if (keys.isEmpty()) return emptyMap()
        val routeIDs = keys.map { it.first }.distinct()
        val stopIDs = keys.map { it.second }.distinct()
        val grouped =
            realtimeRepository
                .findByRouteIDInAndStopIDIn(routeIDs, stopIDs)
                .groupBy { it.routeID to it.stopID }
        return keys.associateWith { key -> grouped[key] ?: emptyList() }
    }

    fun getArrival(
        routeID: Int,
        stopID: Int,
        startStopID: Int,
        limit: Int? = null,
    ): List<BusArrival> {
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val realtimeList = realtimeRepository.findByRouteIDAndStopID(routeID, stopID)
        val timetableList =
            timetableRepository
                .findByRouteIDAndStartStopIDAndWeekdayAndDepartureTimeAfter(
                    routeID,
                    startStopID,
                    resolveWeekday(now.toLocalDate()),
                    now.toLocalTime(),
                    Sort.by(
                        Sort.Order.asc("departureTime"),
                    ),
                )
        val realtimeArrivals =
            realtimeList
                .map {
                    BusArrival(
                        stops = it.remainingStop,
                        seats = it.remainingSeat,
                        minutes = it.remainingTime.toMinutes().toInt(),
                        lowFloor = it.isLowFloor,
                        isRealtime = true,
                    )
                }.sortedBy { it.minutes }
        val timetableArrivals =
            timetableList
                .map {
                    BusArrival(
                        isRealtime = false,
                        time = it.departureTime,
                    )
                }.sortedBy { it.time }
        return (realtimeArrivals + timetableArrivals).take(limit ?: Int.MAX_VALUE)
    }
}
