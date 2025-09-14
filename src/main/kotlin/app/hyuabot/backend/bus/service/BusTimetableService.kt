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
    fun getBusTimetableList(): List<BusTimetable> =
        timetableRepository.findAll(
            Sort.by(
                Sort.Order.asc("route_id"),
                Sort.Order.asc("start_stop_id"),
                Sort.Order.asc("departure_time"),
            ),
        )

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
