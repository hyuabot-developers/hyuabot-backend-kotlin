package app.hyuabot.backend.bus.service

import app.hyuabot.backend.bus.domain.BusTimetableKey
import app.hyuabot.backend.bus.domain.BusTimetableRequest
import app.hyuabot.backend.bus.domain.MinimumDispatchInterval
import app.hyuabot.backend.bus.exception.BusRouteNotFoundException
import app.hyuabot.backend.bus.exception.BusStartStopNotFoundException
import app.hyuabot.backend.bus.exception.BusTimetableNotFoundException
import app.hyuabot.backend.bus.exception.DuplicateBusTimetableException
import app.hyuabot.backend.database.entity.BusTimetable
import app.hyuabot.backend.database.exception.LocalTimeNotValidException
import app.hyuabot.backend.database.repository.BusRouteRepository
import app.hyuabot.backend.database.repository.BusStopRepository
import app.hyuabot.backend.database.repository.BusTimetableRepository
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalTime

@Service
class BusTimetableService(
    private val routeRepository: BusRouteRepository,
    private val stopRepository: BusStopRepository,
    private val timetableRepository: BusTimetableRepository,
) {
    fun getBusTimetableList(
        routeID: Int?,
        startStopID: Int?,
        weekdays: List<String>? = null,
    ): List<BusTimetable> {
        val sort =
            Sort.by(
                Sort.Order.asc("routeID"),
                Sort.Order.asc("startStopID"),
                Sort.Order.asc("departureTime"),
            )
        return when {
            routeID != null && startStopID != null && !weekdays.isNullOrEmpty() -> {
                timetableRepository.findByRouteIDAndStartStopIDAndWeekdayIsIn(
                    routeID,
                    startStopID,
                    weekdays,
                    sort,
                )
            }

            routeID != null && startStopID != null -> {
                timetableRepository.findByRouteIDAndStartStopID(
                    routeID,
                    startStopID,
                    sort,
                )
            }

            routeID != null && !weekdays.isNullOrEmpty() -> {
                timetableRepository.findByRouteIDAndWeekdayIsIn(routeID, weekdays, sort)
            }

            startStopID != null && !weekdays.isNullOrEmpty() -> {
                timetableRepository.findByStartStopIDAndWeekdayIsIn(
                    startStopID,
                    weekdays,
                    sort,
                )
            }

            routeID != null -> {
                timetableRepository.findByRouteID(routeID, sort)
            }

            startStopID != null -> {
                timetableRepository.findByStartStopID(startStopID, sort)
            }

            !weekdays.isNullOrEmpty() -> {
                timetableRepository.findByWeekdayIsIn(weekdays, sort)
            }

            else -> {
                timetableRepository.findAll(sort)
            }
        }
    }

    fun createBusTimetable(payload: BusTimetableRequest): BusTimetable {
        if (!LocalDateTimeBuilder.checkLocalTimeFormat(payload.departureTime)) {
            throw LocalTimeNotValidException()
        }
        routeRepository.findById(payload.routeID).orElseThrow { BusRouteNotFoundException() }
        stopRepository.findById(payload.startStopID).orElseThrow { BusStartStopNotFoundException() }
        timetableRepository
            .findByRouteIDAndStartStopIDAndWeekdayAndDepartureTime(
                routeID = payload.routeID,
                startStopID = payload.startStopID,
                weekday = payload.dayType,
                departureTime = LocalTime.parse(payload.departureTime),
            )?.let {
                throw DuplicateBusTimetableException()
            }
        return timetableRepository.save(
            BusTimetable(
                routeID = payload.routeID,
                startStopID = payload.startStopID,
                weekday = payload.dayType,
                departureTime = LocalTime.parse(payload.departureTime),
            ),
        )
    }

    fun getBusTimetableById(id: Int): BusTimetable = timetableRepository.findById(id).orElseThrow { BusTimetableNotFoundException() }

    fun updateBusTimetable(
        id: Int,
        payload: BusTimetableRequest,
    ): BusTimetable {
        val busTimetable = timetableRepository.findById(id).orElseThrow { BusTimetableNotFoundException() }
        if (!LocalDateTimeBuilder.checkLocalTimeFormat(payload.departureTime)) {
            throw LocalTimeNotValidException()
        }
        routeRepository.findById(payload.routeID).orElseThrow { BusRouteNotFoundException() }
        stopRepository.findById(payload.startStopID).orElseThrow { BusStartStopNotFoundException() }
        timetableRepository
            .findByRouteIDAndStartStopIDAndWeekdayAndDepartureTime(
                routeID = payload.routeID,
                startStopID = payload.startStopID,
                weekday = payload.dayType,
                departureTime = LocalTime.parse(payload.departureTime),
            )?.let {
                throw DuplicateBusTimetableException()
            }
        busTimetable.routeID = payload.routeID
        busTimetable.startStopID = payload.startStopID
        busTimetable.weekday = payload.dayType
        busTimetable.departureTime = LocalTime.parse(payload.departureTime)
        return timetableRepository.save(busTimetable)
    }

    fun deleteBusTimetableById(id: Int) {
        timetableRepository.findById(id).orElseThrow { BusTimetableNotFoundException() }.let { timetable ->
            timetableRepository.delete(timetable)
        }
    }

    fun getBusTimetableBatch(keys: Set<BusTimetableKey>): Map<BusTimetableKey, List<BusTimetable>> {
        if (keys.isEmpty()) return emptyMap()
        val routeIDs = keys.map { it.routeID }.distinct()
        val startStopIDs = keys.map { it.startStopID }.distinct()
        val sort =
            Sort.by(
                Sort.Order.asc("routeID"),
                Sort.Order.asc("startStopID"),
                Sort.Order.asc("departureTime"),
            )
        // Fetch all entries; midnight-aware filtering is done in-memory below
        val grouped =
            timetableRepository
                .findByRouteIDInAndStartStopIDInAndDepartureTimeAfter(
                    routeIDs,
                    startStopIDs,
                    LocalTime.MIN,
                    sort,
                ).groupBy { it.routeID to it.startStopID }
        return keys.associateWith { key ->
            val timetables = grouped[key.routeID to key.startStopID] ?: emptyList()
            val filtered =
                when {
                    key.weekdays.isNullOrEmpty() && key.after != null -> {
                        timetables.filter { it.departureTime.toServiceMinutes() > key.after.toServiceMinutes() }
                    }

                    !key.weekdays.isNullOrEmpty() && key.after != null -> {
                        timetables.filter {
                            it.weekday in key.weekdays && it.departureTime.toServiceMinutes() > key.after.toServiceMinutes()
                        }
                    }

                    !key.weekdays.isNullOrEmpty() -> {
                        timetables.filter { it.weekday in key.weekdays }
                    }

                    else -> {
                        timetables
                    }
                }
            filtered.sortedBy { it.departureTime.toServiceMinutes() }
        }
    }

    fun getMinimumDispatchIntervalsBatch(keys: Set<Pair<Int, Int>>): Map<Pair<Int, Int>, List<MinimumDispatchInterval>> {
        if (keys.isEmpty()) return emptyMap()
        val routeIDs = keys.map { it.first }.distinct()
        val startStopIDs = keys.map { it.second }.distinct()
        val sort =
            Sort.by(
                Sort.Order.asc("routeID"),
                Sort.Order.asc("startStopID"),
                Sort.Order.asc("departureTime"),
            )
        val grouped =
            timetableRepository
                .findByRouteIDInAndStartStopIDInAndDepartureTimeAfter(
                    routeIDs,
                    startStopIDs,
                    LocalTime.MIN,
                    sort,
                ).groupBy { it.routeID to it.startStopID }
        return keys.associateWith { key ->
            grouped[key]
                .orEmpty()
                .groupBy { it.weekday }
                .mapNotNull { (weekday, entries) ->
                    val times = entries.map { it.departureTime.toServiceMinutes() }.sorted()
                    val minimumGap =
                        times
                            .zipWithNext { first, second -> (second - first) / 60 }
                            .filter { it > 0 }
                            .minOrNull()
                    minimumGap?.let { MinimumDispatchInterval(weekday, it) }
                }.sortedBy { it.weekday }
        }
    }

    companion object {
        val SERVICE_DAY_START: LocalTime = LocalTime.of(4, 0)

        fun LocalTime.toServiceMinutes(): Int {
            val seconds = this.toSecondOfDay()
            val threshold = SERVICE_DAY_START.toSecondOfDay()
            return if (seconds >= threshold) seconds else seconds + 24 * 60 * 60
        }
    }
}
