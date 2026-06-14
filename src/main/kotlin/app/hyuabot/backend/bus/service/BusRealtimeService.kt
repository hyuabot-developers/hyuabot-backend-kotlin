package app.hyuabot.backend.bus.service

import app.hyuabot.backend.bus.domain.BusArrivalKey
import app.hyuabot.backend.codegen.types.BusArrival
import app.hyuabot.backend.database.entity.BusRealtime
import app.hyuabot.backend.database.repository.BusDepartureLogRepository
import app.hyuabot.backend.database.repository.BusRealtimeRepository
import app.hyuabot.backend.database.repository.BusTimetableRepository
import app.hyuabot.backend.holiday.service.PublicHolidayService
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
    private val departureLogRepository: BusDepartureLogRepository,
    private val timetableRepository: BusTimetableRepository,
    private val publicHolidayService: PublicHolidayService,
) {
    private val serviceStartTime: LocalTime = LocalTime.of(4, 0)

    internal fun resolveWeekday(date: LocalDate): String {
        if (publicHolidayService.findPublicHoliday(date) != null) return "sunday"
        return when (date.dayOfWeek) {
            DayOfWeek.SATURDAY -> "saturday"
            DayOfWeek.SUNDAY -> "sunday"
            else -> "weekdays"
        }
    }

    internal fun toServiceSeconds(time: LocalTime): Int {
        val seconds = time.toSecondOfDay()
        val threshold = serviceStartTime.toSecondOfDay()
        return if (seconds >= threshold) seconds else seconds + 24 * 60 * 60
    }

    internal fun clusterDepartureTimes(
        times: List<LocalTime>,
        thresholdMinutes: Int = 3,
    ): List<LocalTime> {
        if (times.isEmpty()) return emptyList()
        val sorted = times.sortedBy { toServiceSeconds(it) }
        val clusters = mutableListOf<MutableList<LocalTime>>()
        var current = mutableListOf(sorted.first())
        for (i in 1 until sorted.size) {
            val diff = toServiceSeconds(sorted[i]) - toServiceSeconds(current.last())
            if (diff <= thresholdMinutes * 60) {
                current.add(sorted[i])
            } else {
                clusters.add(current)
                current = mutableListOf(sorted[i])
            }
        }
        clusters.add(current)
        return clusters.map { cluster ->
            val avgSeconds = cluster.map { toServiceSeconds(it).toLong() }.average().toLong()
            LocalTime.ofSecondOfDay(avgSeconds % 86400L)
        }
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
        val routeIDs = keys.map { it.routeID }.distinct()
        val stopIDs = keys.map { it.stopID }.distinct()
        val startStopIDs = keys.map { it.startStopID }.distinct()
        val realtimeGrouped =
            realtimeRepository
                .findByRouteIDInAndStopIDIn(routeIDs, stopIDs)
                .groupBy { it.routeID to it.stopID }
        val serviceDate =
            if (currentTime.isBefore(serviceStartTime)) now.toLocalDate().minusDays(1) else now.toLocalDate()
        val weekday = resolveWeekday(serviceDate)
        val sameDayDates = (1..4).map { serviceDate.minusWeeks(it.toLong()) }
        val logGrouped =
            departureLogRepository
                .findByRouteIDInAndStopIDInAndDepartureDateIn(routeIDs, stopIDs, sameDayDates)
                .groupBy { it.routeID to it.stopID }
        val sort = Sort.by(Sort.Order.asc("routeID"), Sort.Order.asc("startStopID"), Sort.Order.asc("departureTime"))
        val timetableGrouped =
            timetableRepository
                .findByRouteIDInAndStartStopIDInAndWeekdayAndDepartureTimeAfter(
                    routeIDs,
                    startStopIDs,
                    weekday,
                    LocalTime.MIN,
                    sort,
                ).groupBy { it.routeID to it.startStopID }
        return keys.associateWith { key ->
            val rawRealtimes = realtimeGrouped[key.routeID to key.stopID] ?: emptyList()
            val sortedRealtimes = rawRealtimes.sortedBy { it.remainingTime.toMinutes().toInt() }
            val lastRealtimeMinutes =
                if (sortedRealtimes.isEmpty()) {
                    -10
                } else {
                    sortedRealtimes
                        .last()
                        .remainingTime
                        .toMinutes()
                        .toInt()
                }
            val cutoffMinutes = lastRealtimeMinutes + 10
            val realtimeArrivals =
                sortedRealtimes.map {
                    BusArrival(
                        stops = it.remainingStop,
                        seats = it.remainingSeat,
                        minutes = it.remainingTime.toMinutes().toInt(),
                        lowFloor = it.isLowFloor,
                        isRealtime = true,
                    )
                }
            val rawLogTimes =
                (logGrouped[key.routeID to key.stopID] ?: emptyList())
                    .map { it.departureTime }
            val logArrivals =
                clusterDepartureTimes(rawLogTimes)
                    .filter { time -> (toServiceSeconds(time) - toServiceSeconds(currentTime)) / 60 > cutoffMinutes }
                    .map { logTime ->
                        val terminalTime = logTime.minusMinutes(key.minuteFromStart.toLong())
                        BusArrival(isRealtime = false, time = terminalTime, arrivalTime = logTime)
                    }.sortedBy { toServiceSeconds(it.time!!) }
            val scheduledArrivals =
                if (logArrivals.isEmpty()) {
                    (timetableGrouped[key.routeID to key.startStopID] ?: emptyList())
                        .filter { timetable ->
                            val estimatedArrival = timetable.departureTime.plusMinutes(key.minuteFromStart.toLong())
                            (toServiceSeconds(estimatedArrival) - toServiceSeconds(currentTime)) / 60 > cutoffMinutes
                        }.map { timetable ->
                            val estimatedArrival = timetable.departureTime.plusMinutes(key.minuteFromStart.toLong())
                            BusArrival(isRealtime = false, time = timetable.departureTime, arrivalTime = estimatedArrival)
                        }.sortedBy { toServiceSeconds(it.arrivalTime!!) }
                } else {
                    logArrivals
                }
            (realtimeArrivals + scheduledArrivals).take(key.limit ?: Int.MAX_VALUE)
        }
    }
}
