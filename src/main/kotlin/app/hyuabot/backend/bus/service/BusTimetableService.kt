package app.hyuabot.backend.bus.service

import app.hyuabot.backend.bus.domain.BusTimetableRequest
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
        weekdays: String?,
    ): List<BusTimetable> {
        val sort =
            Sort.by(
                Sort.Order.asc("routeID"),
                Sort.Order.asc("startStopID"),
                Sort.Order.asc("departureTime"),
            )
        return when {
            routeID != null && startStopID != null && weekdays != null -> {
                timetableRepository.findByRouteIDAndStartStopIDAndWeekday(
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

            routeID != null && weekdays != null -> {
                timetableRepository.findByRouteIDAndWeekday(routeID, weekdays, sort)
            }

            startStopID != null && weekdays != null -> {
                timetableRepository.findByStartStopIDAndWeekday(
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

            weekdays != null -> {
                timetableRepository.findByWeekday(weekdays, sort)
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
        val updatedBusTimetable =
            busTimetable.copy(
                routeID = payload.routeID,
                startStopID = payload.startStopID,
                weekday = payload.dayType,
                departureTime = LocalTime.parse(payload.departureTime),
            )
        return timetableRepository.save(updatedBusTimetable)
    }

    fun deleteBusTimetableById(id: Int) {
        timetableRepository.findById(id).orElseThrow { BusTimetableNotFoundException() }.let { timetable ->
            timetableRepository.delete(timetable)
        }
    }
}
