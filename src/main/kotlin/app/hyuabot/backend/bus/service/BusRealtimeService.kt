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
import java.time.LocalTime
import java.time.ZoneId

@Service
class BusRealtimeService(
    private val realtimeRepository: BusRealtimeRepository,
    private val timetableRepository: BusTimetableRepository,
) {
    private val serviceStartTime: LocalTime = LocalTime.of(4, 0)

    internal fun resolveWeekday(date: LocalDate): String =
        when (date.dayOfWeek) {
            DayOfWeek.SATURDAY -> "saturday"
            DayOfWeek.SUNDAY -> "sunday"
            else -> "weekdays"
        }

    private fun toServiceMinutes(time: LocalTime): Int {
        val mins = time.hour * 60 + time.minute
        val threshold = serviceStartTime.hour * 60 + serviceStartTime.minute
        return if (mins >= threshold) mins else mins + 24 * 60
    }

    internal fun currentTime(): LocalDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))

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
        val now = currentTime()
        val currentTime = now.toLocalTime()
        val sort = Sort.by(Sort.Order.asc("departureTime"))
        val routeIDs = keys.map { it.routeID }.distinct()
        val stopIDs = keys.map { it.stopID }.distinct()
        val startStopIDs = keys.map { it.startStopID }.distinct()
        val realtimeGrouped =
            realtimeRepository
                .findByRouteIDInAndStopIDIn(routeIDs, stopIDs)
                .groupBy { it.routeID to it.stopID }
        // Times before serviceStartTime (04:00) belong to the previous calendar day's service
        val serviceDate =
            if (currentTime.isBefore(serviceStartTime)) {
                now.toLocalDate().minusDays(1)
            } else {
                now.toLocalDate()
            }
        val weekday = resolveWeekday(serviceDate)
        val timetableEntries =
            if (currentTime.isBefore(serviceStartTime)) {
                // After midnight: remaining buses are between currentTime and serviceStartTime
                timetableRepository
                    .findByRouteIDInAndStartStopIDInAndWeekdayAndDepartureTimeAfter(
                        routeIDs,
                        startStopIDs,
                        weekday,
                        currentTime,
                        sort,
                    ).filter { it.departureTime.isBefore(serviceStartTime) }
            } else {
                // Normal hours: buses from now until midnight + after-midnight buses (00:00–serviceStartTime)
                val remaining =
                    timetableRepository.findByRouteIDInAndStartStopIDInAndWeekdayAndDepartureTimeAfter(
                        routeIDs,
                        startStopIDs,
                        weekday,
                        currentTime,
                        sort,
                    )
                val afterMidnight =
                    timetableRepository.findByRouteIDInAndStartStopIDInAndWeekdayAndDepartureTimeBefore(
                        routeIDs,
                        startStopIDs,
                        weekday,
                        serviceStartTime,
                        sort,
                    )
                remaining + afterMidnight
            }
        val timetableGrouped = timetableEntries.groupBy { it.routeID to it.startStopID }
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
                    .sortedBy { toServiceMinutes(it.time!!) }
            (realtimeArrivals + timetableArrivals).take(key.limit ?: Int.MAX_VALUE)
        }
    }
}
