package app.hyuabot.backend.bus.service

import app.hyuabot.backend.bus.domain.BusRouteStopRequest
import app.hyuabot.backend.bus.domain.CreateBusRouteRequest
import app.hyuabot.backend.bus.domain.UpdateBusRouteRequest
import app.hyuabot.backend.bus.exception.BusEndStopNotFoundException
import app.hyuabot.backend.bus.exception.BusRouteNotFoundException
import app.hyuabot.backend.bus.exception.BusRouteStopNotFoundException
import app.hyuabot.backend.bus.exception.BusStartStopNotFoundException
import app.hyuabot.backend.bus.exception.BusStopNotFoundException
import app.hyuabot.backend.bus.exception.DuplicateBusRouteException
import app.hyuabot.backend.bus.exception.DuplicateBusRouteStopException
import app.hyuabot.backend.codegen.types.BusRouteStopInput
import app.hyuabot.backend.database.entity.BusDepartureLog
import app.hyuabot.backend.database.entity.BusRoute
import app.hyuabot.backend.database.entity.BusRouteStop
import app.hyuabot.backend.database.exception.LocalTimeNotValidException
import app.hyuabot.backend.database.repository.BusDepartureLogRepository
import app.hyuabot.backend.database.repository.BusRouteRepository
import app.hyuabot.backend.database.repository.BusRouteStopRepository
import app.hyuabot.backend.database.repository.BusStopRepository
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime

@Service
class BusRouteService(
    private val routeRepository: BusRouteRepository,
    private val stopRepository: BusStopRepository,
    private val routeStopRepository: BusRouteStopRepository,
    private val departureLogRepository: BusDepartureLogRepository,
) {
    fun getBusRouteList(): List<BusRoute> = routeRepository.findAll().sortedBy { it.name }

    fun createBusRoute(payload: CreateBusRouteRequest): BusRoute {
        if (
            !LocalDateTimeBuilder.checkLocalTimeFormat(payload.upFirstTime) ||
            !LocalDateTimeBuilder.checkLocalTimeFormat(payload.upLastTime) ||
            !LocalDateTimeBuilder.checkLocalTimeFormat(payload.downFirstTime) ||
            !LocalDateTimeBuilder.checkLocalTimeFormat(payload.downLastTime)
        ) {
            throw LocalTimeNotValidException()
        }
        routeRepository.findById(payload.id).orElse(null)?.let {
            throw DuplicateBusRouteException()
        }
        stopRepository.findById(payload.startStopID).orElseThrow { throw BusStartStopNotFoundException() }
        stopRepository.findById(payload.endStopID).orElseThrow { throw BusEndStopNotFoundException() }
        return routeRepository.save(
            BusRoute(
                id = payload.id,
                name = payload.name,
                typeCode = payload.typeCode,
                typeName = payload.typeName,
                startStopID = payload.startStopID,
                endStopID = payload.endStopID,
                upFirstTime = LocalTime.parse(payload.upFirstTime),
                upLastTime = LocalTime.parse(payload.upLastTime),
                downFirstTime = LocalTime.parse(payload.downFirstTime),
                downLastTime = LocalTime.parse(payload.downLastTime),
                districtCode = payload.districtCode,
                companyID = payload.companyID,
                companyName = payload.companyName,
                companyPhone = payload.companyPhone,
                stop = emptyList(),
            ),
        )
    }

    fun getBusRouteById(id: Int): BusRoute = routeRepository.findById(id).orElseThrow { throw BusRouteNotFoundException() }

    fun updateBusRoute(
        id: Int,
        payload: UpdateBusRouteRequest,
    ): BusRoute {
        val busRoute = routeRepository.findById(id).orElseThrow { throw BusRouteNotFoundException() }
        if (
            !LocalDateTimeBuilder.checkLocalTimeFormat(payload.upFirstTime) ||
            !LocalDateTimeBuilder.checkLocalTimeFormat(payload.upLastTime) ||
            !LocalDateTimeBuilder.checkLocalTimeFormat(payload.downFirstTime) ||
            !LocalDateTimeBuilder.checkLocalTimeFormat(payload.downLastTime)
        ) {
            throw LocalTimeNotValidException()
        }
        stopRepository.findById(payload.startStopID).orElseThrow { throw BusStartStopNotFoundException() }
        stopRepository.findById(payload.endStopID).orElseThrow { throw BusEndStopNotFoundException() }
        val updatedBusRoute =
            busRoute.copy(
                name = payload.name,
                typeCode = payload.typeCode,
                typeName = payload.typeName,
                startStopID = payload.startStopID,
                endStopID = payload.endStopID,
                upFirstTime = LocalTime.parse(payload.upFirstTime),
                upLastTime = LocalTime.parse(payload.upLastTime),
                downFirstTime = LocalTime.parse(payload.downFirstTime),
                downLastTime = LocalTime.parse(payload.downLastTime),
                districtCode = payload.districtCode,
                companyID = payload.companyID,
                companyName = payload.companyName,
                companyPhone = payload.companyPhone,
            )
        return routeRepository.save(updatedBusRoute)
    }

    fun deleteBusRouteById(id: Int) {
        val busRoute = routeRepository.findById(id).orElseThrow { throw BusRouteNotFoundException() }
        routeStopRepository.deleteAll(busRoute.stop)
        routeRepository.delete(busRoute)
    }

    fun getBusStopListByRouteID(routeID: Int) =
        routeRepository
            .findById(routeID)
            .orElseThrow {
                throw BusRouteNotFoundException()
            }.stop
            .sortedBy { it.order }

    fun fetchRouteStops(keys: List<BusRouteStopInput>): List<BusRouteStop> {
        if (keys.isEmpty()) return emptyList()
        val routes = keys.map { it.route }.distinct()
        val orders = keys.map { it.order }.distinct()
        val keySet = keys.map { it.route to it.order }.toSet()
        return routeStopRepository.fetchBusRouteStops(routes, orders).filter { rs ->
            (rs.routeID to rs.order) in keySet
        }
    }

    fun createBusRouteStop(
        routeID: Int,
        payload: BusRouteStopRequest,
    ): BusRouteStop {
        routeRepository.findById(routeID).orElseThrow { throw BusRouteNotFoundException() }
        stopRepository.findById(payload.stopID).orElseThrow { throw BusStopNotFoundException() }
        stopRepository.findById(payload.startStopID).orElseThrow { throw BusStartStopNotFoundException() }
        routeStopRepository.findByRouteIDAndOrder(routeID, payload.order)?.let {
            throw DuplicateBusRouteStopException()
        }
        return routeStopRepository.save(
            BusRouteStop(
                routeID = routeID,
                stopID = payload.stopID,
                order = payload.order,
                startStopID = payload.startStopID,
                minuteFromStart = payload.travelTime,
                route = null,
                stop = null,
                startStop = null,
                log = emptyList(),
                realtime = emptyList(),
            ),
        )
    }

    fun updateBusRouteStop(
        routeID: Int,
        seq: Int,
        payload: BusRouteStopRequest,
    ): BusRouteStop {
        routeRepository.findById(routeID).orElseThrow { throw BusRouteNotFoundException() }
        val stop =
            routeStopRepository.findByRouteIDAndSeq(routeID, seq) ?: throw BusRouteStopNotFoundException()
        stopRepository.findById(payload.stopID).orElseThrow { throw BusStopNotFoundException() }
        stopRepository.findById(payload.startStopID).orElseThrow { throw BusStartStopNotFoundException() }
        routeStopRepository.findByRouteIDAndOrderAndSeqNot(routeID, payload.order, seq)?.let {
            throw DuplicateBusRouteStopException()
        }
        return routeStopRepository.save(
            stop.copy(
                stopID = payload.stopID,
                order = payload.order,
                startStopID = payload.startStopID,
                minuteFromStart = payload.travelTime,
            ),
        )
    }

    fun deleteBusRouteStopBySeq(
        routeID: Int,
        seq: Int,
    ) {
        routeRepository.findById(routeID).orElseThrow { throw BusRouteNotFoundException() }
        val stop = routeStopRepository.findByRouteIDAndSeq(routeID, seq) ?: throw BusRouteStopNotFoundException()
        routeStopRepository.delete(stop)
    }

    fun getBusDepartureLogByRouteStop(
        routeID: Int,
        seq: Int,
    ): List<BusDepartureLog> {
        routeRepository.findById(routeID).orElseThrow { throw BusRouteNotFoundException() }
        val stop = routeStopRepository.findByRouteIDAndSeq(routeID, seq) ?: throw BusRouteStopNotFoundException()
        return departureLogRepository.findByRouteIDAndStopID(routeID, stop.stopID)
    }

    fun getBusDepartureLogByRouteStopAndDate(
        routeID: Int,
        stopID: Int,
        dates: List<LocalDate>,
    ): List<BusDepartureLog> =
        departureLogRepository.findByRouteIDAndStopIDAndDepartureDateIsIn(
            routeID,
            stopID,
            dates,
        )
}
