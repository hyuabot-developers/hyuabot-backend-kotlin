package app.hyuabot.backend.bus.service

import app.hyuabot.backend.bus.domain.BusArrivalKey
import app.hyuabot.backend.bus.domain.BusDepartureLogKey
import app.hyuabot.backend.codegen.types.BusArrival
import app.hyuabot.backend.codegen.types.BusDestinationTravelMinutes
import app.hyuabot.backend.database.entity.BusRealtime
import app.hyuabot.backend.database.repository.BusDepartureLogRepository
import app.hyuabot.backend.database.repository.BusRealtimeRepository
import app.hyuabot.backend.database.repository.BusTimetableRepository
import app.hyuabot.backend.holiday.service.PublicHolidayService
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.abs

@Service
class BusRealtimeService(
    private val realtimeRepository: BusRealtimeRepository,
    private val departureLogRepository: BusDepartureLogRepository,
    private val timetableRepository: BusTimetableRepository,
    private val publicHolidayService: PublicHolidayService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private var cacheManager: CacheManager = ConcurrentMapCacheManager("busTravelTime")

    @Autowired
    fun setCacheManager(cacheManager: CacheManager) {
        this.cacheManager = cacheManager
    }

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

    /** Median gap (minutes) between consecutive scheduled departures — this route's typical dispatch interval. */
    internal fun estimateHeadwayMinutes(departureTimes: List<LocalTime>): Int? {
        if (departureTimes.size < 2) return null
        val sortedSeconds = departureTimes.map { toServiceSeconds(it) }.sorted()
        val gapMinutes = sortedSeconds.zipWithNext { a, b -> (b - a) / 60 }.filter { it > 0 }
        if (gapMinutes.isEmpty()) return null
        val sortedGaps = gapMinutes.sorted()
        val mid = sortedGaps.size / 2
        return if (sortedGaps.size % 2 == 0) (sortedGaps[mid - 1] + sortedGaps[mid]) / 2 else sortedGaps[mid]
    }

    /**
     * Departure-log clustering window: a fraction of the route's own headway, so frequent routes
     * (short headway) don't blur two distinct dispatches together, while infrequent routes get more
     * slack to absorb day-to-day timing jitter.
     */
    internal fun clusterThresholdMinutes(headwayMinutes: Int?): Int = (((headwayMinutes ?: 12) / 3).coerceIn(2, 12))

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

    internal fun currentTime(): LocalDateTime = LocalDateTime.now(LocalDateTimeBuilder.serviceTimezone)

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
        // Include the current service day so arrival can use the same-day departure
        // logs that the realtime screen receives through the log field. Keep three
        // comparable prior weeks for fallback and travel-time estimation.
        val sameDayDates = (0..3).map { serviceDate.minusWeeks(it.toLong()) }
        val logKeys =
            keys
                .flatMap { key ->
                    buildList {
                        add(BusDepartureLogKey(key.routeID, key.stopID, sameDayDates))
                        (key.destinationStopIDs + listOfNotNull(key.destinationStopID)).forEach { destinationStopID ->
                            add(BusDepartureLogKey(key.routeID, destinationStopID, sameDayDates))
                        }
                    }
                }
        val allLogs = departureLogRepository.findByRouteStopAndDepartureDates(logKeys.toSet())
        val logGrouped = allLogs.groupBy { it.routeID to it.stopID }
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
                        destinationTravelMinutes = emptyList(),
                    )
                }
            val timetableEntries = timetableGrouped[key.routeID to key.startStopID] ?: emptyList()
            val headwayMinutes = estimateHeadwayMinutes(timetableEntries.map { it.departureTime })
            val rawLogTimes =
                (logGrouped[key.routeID to key.stopID] ?: emptyList())
                    .map { it.departureTime }
            val logArrivals =
                clusterDepartureTimes(rawLogTimes, clusterThresholdMinutes(headwayMinutes))
                    .filter { time ->
                        val remainingSeconds = toServiceSeconds(time) - toServiceSeconds(currentTime)
                        remainingSeconds / 60 > cutoffMinutes &&
                            remainingSeconds >= key.minuteFromStart * 60
                    }
                    .map { logTime ->
                        val estimatedTerminalTime = logTime.minusMinutes(key.minuteFromStart.toLong())
                        var terminalTime = estimatedTerminalTime
                        var bestDiff = Int.MAX_VALUE
                        for (entry in timetableEntries) {
                            val diff = abs(toServiceSeconds(entry.departureTime) - toServiceSeconds(estimatedTerminalTime))
                            if (diff < bestDiff) {
                                bestDiff = diff
                                terminalTime = entry.departureTime
                            }
                        }
                        BusArrival(
                            isRealtime = false,
                            time = terminalTime,
                            arrivalTime = logTime,
                            destinationTravelMinutes = emptyList(),
                        )
                    }.sortedBy { toServiceSeconds(it.time!!) }
            val scheduledArrivals =
                if (logArrivals.isEmpty()) {
                    timetableEntries
                        .filter { timetable ->
                            val estimatedArrival = timetable.departureTime.plusMinutes(key.minuteFromStart.toLong())
                            val remainingSeconds = toServiceSeconds(estimatedArrival) - toServiceSeconds(currentTime)
                            remainingSeconds / 60 > cutoffMinutes &&
                                remainingSeconds >= key.minuteFromStart * 60
                        }.map { timetable ->
                            val estimatedArrival = timetable.departureTime.plusMinutes(key.minuteFromStart.toLong())
                            BusArrival(
                                isRealtime = false,
                                time = timetable.departureTime,
                                arrivalTime = estimatedArrival,
                                destinationTravelMinutes = emptyList(),
                            )
                        }.sortedBy { toServiceSeconds(it.arrivalTime!!) }
                } else {
                    logArrivals
                }
            val arrivals = (realtimeArrivals + scheduledArrivals).take(key.limit ?: Int.MAX_VALUE)
            val sourceLogs = logGrouped[key.routeID to key.stopID].orEmpty()
            val destinationStopIDs = (key.destinationStopIDs + listOfNotNull(key.destinationStopID)).distinct()
            arrivals.map { arrival ->
                val primaryTime = arrival.arrivalTime ?: currentTime.plusMinutes(arrival.minutes!!.toLong())
                val destinationTravelMinutes =
                    destinationStopIDs.mapNotNull { destinationStopID ->
                        estimateDestinationTravelMinutes(
                            key = key,
                            destinationStopID = destinationStopID,
                            primaryTime = primaryTime,
                            sourceLogs = sourceLogs,
                            destinationLogs = logGrouped[key.routeID to destinationStopID].orEmpty(),
                        )?.let { minutes ->
                            BusDestinationTravelMinutes(
                                destinationStopId = destinationStopID,
                                minutes = minutes,
                            )
                        }
                    }
                arrival.copy(
                    destinationArrivalTime =
                        destinationTravelMinutes
                            .firstOrNull { it.destinationStopId == key.destinationStopID }
                            ?.minutes
                            ?.let { primaryTime.plusMinutes(it.toLong()) },
                    destinationTravelMinutes = destinationTravelMinutes,
                )
            }
        }
    }

    private fun estimateDestinationTravelMinutes(
        key: BusArrivalKey,
        destinationStopID: Int,
        primaryTime: LocalTime,
        sourceLogs: List<app.hyuabot.backend.database.entity.BusDepartureLog>,
        destinationLogs: List<app.hyuabot.backend.database.entity.BusDepartureLog>,
    ): Int? {
        val durations = cachedTravelDurations(key.routeID, key.stopID, destinationStopID, sourceLogs, destinationLogs)
        if (durations.isEmpty()) {
            logger.info(
                "Bus destination ETA diagnostic route={} sourceStop={} destinationStop={} " +
                    "primaryTime={} reason=empty_durations sourceLogs={} destinationLogs={}",
                key.routeID,
                key.stopID,
                destinationStopID,
                primaryTime,
                sourceLogs.size,
                destinationLogs.size,
            )
            return null
        }
        val targetBucket = primaryTime.toSecondOfDay() / TRAVEL_TIME_BUCKET_SECONDS
        val duration =
            listOf(0, 1, -1, 2, -2, 3, -3, 4, -4)
                .asSequence()
                .mapNotNull { offset -> durations[targetBucket + offset] }
                .firstOrNull()
                ?: run {
                    logger.info(
                        "Bus destination ETA diagnostic route={} sourceStop={} destinationStop={} " +
                            "primaryTime={} targetBucket={} reason=target_bucket_missing durationBuckets={}",
                        key.routeID,
                        key.stopID,
                        destinationStopID,
                        primaryTime,
                        targetBucket,
                        durations.keys.sorted(),
                    )
                    return null
                }
        return duration
    }

    @Suppress("UNCHECKED_CAST")
    private fun cachedTravelDurations(
        routeID: Int,
        sourceStopID: Int,
        destinationStopID: Int,
        sourceLogs: List<app.hyuabot.backend.database.entity.BusDepartureLog>,
        destinationLogs: List<app.hyuabot.backend.database.entity.BusDepartureLog>,
    ): Map<Int, Int> {
        val cacheKey =
            listOf(routeID, sourceStopID, destinationStopID, sourceLogs.map { it.departureDate }.distinct().sorted())
                .joinToString(":")
        val cache = checkNotNull(cacheManager.getCache("busTravelTime"))
        val cached = cache.get(cacheKey)?.get() as? Map<*, *>
        if (cached != null) {
            val normalized =
                cached.entries
                    .mapNotNull { entry ->
                        val bucket = entry.key.toString().toIntOrNull()
                        val duration = entry.value.toString().toIntOrNull()
                        if (bucket != null && duration != null) bucket to duration else null
                    }.toMap()
            logger.debug(
                "Bus destination ETA cache hit route={} sourceStop={} destinationStop={} " +
                    "cacheKey={} durationBuckets={}",
                routeID,
                sourceStopID,
                destinationStopID,
                cacheKey,
                normalized.keys.sorted(),
            )
            return normalized
        }
        val destinationByDate = destinationLogs.groupBy { it.departureDate }
        val samples =
            sourceLogs.mapNotNull { source ->
                destinationByDate[source.departureDate]
                    .orEmpty()
                    .filter { destination -> destination.vehicleID == source.vehicleID && destination.departureTime > source.departureTime }
                    .minByOrNull { it.departureTime }
                    ?.let { destination ->
                        val duration =
                            java.time.Duration
                                .between(source.departureTime, destination.departureTime)
                                .toMinutes()
                                .toInt()
                        if (duration in 1 until MAX_TRAVEL_TIME_MINUTES) {
                            source.departureTime.toSecondOfDay() / TRAVEL_TIME_BUCKET_SECONDS to duration
                        } else {
                            null
                        }
                    }
            }
        val durations = samples.groupBy({ it.first }, { it.second }).mapValues { (_, values) -> values.average().toInt() }
        logger.info(
            "Bus destination ETA cache miss route={} sourceStop={} destinationStop={} " +
                "cacheKey={} sourceLogs={} destinationLogs={} samples={} durationBuckets={}",
            routeID,
            sourceStopID,
            destinationStopID,
            cacheKey,
            sourceLogs.size,
            destinationLogs.size,
            samples.size,
            durations.keys.sorted(),
        )
        cache.put(cacheKey, durations)
        return durations
    }

    private companion object {
        const val TRAVEL_TIME_BUCKET_SECONDS = 30 * 60
        const val MAX_TRAVEL_TIME_MINUTES = 180
    }
}
