package app.hyuabot.backend.bus.service

import app.hyuabot.backend.bus.domain.BusArrivalKey
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

    fun getArrivalBatch(keys: Set<BusArrivalKey>): Map<BusArrivalKey, List<BusArrival>> {
        if (keys.isEmpty()) return emptyMap()
        val now = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val weekday = resolveWeekday(now.toLocalDate())
        val sort = Sort.by(Sort.Order.asc("departureTime"))
        val routeIDs = keys.map { it.routeID }.distinct()
        val stopIDs = keys.map { it.stopID }.distinct()
        val startStopIDs = keys.map { it.startStopID }.distinct()
        val realtimeGrouped =
            realtimeRepository
                .findByRouteIDInAndStopIDIn(routeIDs, stopIDs)
                .groupBy { it.routeID to it.stopID }
        val timetableGrouped =
            timetableRepository
                .findByRouteIDInAndStartStopIDInAndWeekdayAndDepartureTimeAfter(
                    routeIDs,
                    startStopIDs,
                    weekday,
                    now.toLocalTime(),
                    sort,
                ).groupBy { it.routeID to it.startStopID }
        return keys.associateWith { key ->
            val realtimeArrivals =
                (realtimeGrouped[key.routeID to key.stopID] ?: emptyList())
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
                (timetableGrouped[key.routeID to key.startStopID] ?: emptyList())
                    .map { BusArrival(isRealtime = false, time = it.departureTime) }
                    .sortedBy { it.time }
            (realtimeArrivals + timetableArrivals).take(key.limit ?: Int.MAX_VALUE)
        }
    }
}
