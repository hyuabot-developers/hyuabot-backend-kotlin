package app.hyuabot.backend.subway.service

import app.hyuabot.backend.database.entity.SubwayRoute
import app.hyuabot.backend.database.entity.SubwayRouteStation
import app.hyuabot.backend.database.entity.SubwayStation
import app.hyuabot.backend.database.entity.SubwayTimetable
import app.hyuabot.backend.database.exception.DurationNotValidException
import app.hyuabot.backend.database.exception.LocalTimeNotValidException
import app.hyuabot.backend.database.repository.SubwayRealtimeRepository
import app.hyuabot.backend.database.repository.SubwayRouteRepository
import app.hyuabot.backend.database.repository.SubwayStationNameRepository
import app.hyuabot.backend.database.repository.SubwayStationRepository
import app.hyuabot.backend.database.repository.SubwayTimetableRepository
import app.hyuabot.backend.subway.domain.CreateSubwayRouteRequest
import app.hyuabot.backend.subway.domain.CreateSubwayStationRequest
import app.hyuabot.backend.subway.domain.SubwayTimetableRequest
import app.hyuabot.backend.subway.domain.UpdateSubwayRouteRequest
import app.hyuabot.backend.subway.domain.UpdateSubwayStationRequest
import app.hyuabot.backend.subway.exception.DuplicateSubwayRouteException
import app.hyuabot.backend.subway.exception.DuplicateSubwayStationException
import app.hyuabot.backend.subway.exception.DuplicateSubwayTimetableException
import app.hyuabot.backend.subway.exception.SubwayRouteNotFoundException
import app.hyuabot.backend.subway.exception.SubwayStartStationNotFoundException
import app.hyuabot.backend.subway.exception.SubwayStationNotFoundException
import app.hyuabot.backend.subway.exception.SubwayTerminalStationNotFoundException
import app.hyuabot.backend.subway.exception.SubwayTimetableNotFoundException
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import org.springframework.stereotype.Service
import java.time.LocalTime
import kotlin.collections.emptyList

@Service
class SubwayService(
    private val nameRepository: SubwayStationNameRepository,
    private val stationRepository: SubwayStationRepository,
    private val routeRepository: SubwayRouteRepository,
    private val timetableRepository: SubwayTimetableRepository,
    private val realtimeRepository: SubwayRealtimeRepository,
) {
    fun getSubwayRoutes() = routeRepository.findAll()

    fun createSubwayRoute(payload: CreateSubwayRouteRequest): SubwayRoute {
        routeRepository.findById(payload.id).orElse(null)?.let {
            throw DuplicateSubwayRouteException()
        }
        return routeRepository.save(
            SubwayRoute(
                id = payload.id,
                name = payload.name,
                station = emptyList(),
            ),
        )
    }

    fun getSubwayRouteById(id: Int): SubwayRoute = routeRepository.findById(id).orElseThrow { SubwayRouteNotFoundException() }

    fun updateSubwayRoute(
        id: Int,
        payload: UpdateSubwayRouteRequest,
    ): SubwayRoute {
        val route = routeRepository.findById(id).orElseThrow { SubwayRouteNotFoundException() }
        route.let {
            it.name = payload.name
            return routeRepository.save(it)
        }
    }

    fun deleteSubwayRoute(id: Int) {
        val route = routeRepository.findById(id).orElseThrow { SubwayRouteNotFoundException() }
        timetableRepository.deleteAll(route.station.flatMap { it.timetable ?: emptyList() })
        realtimeRepository.deleteAll(route.station.flatMap { it.realtime ?: emptyList() })
        stationRepository.deleteAll(route.station)
        routeRepository.delete(route)
    }

    fun getAllStations() = stationRepository.findAll()

    fun createStation(payload: CreateSubwayStationRequest): SubwayRouteStation {
        if (!LocalDateTimeBuilder.checkLocalTimeFormat(payload.cumulativeTime)) {
            throw DurationNotValidException()
        }
        stationRepository.findById(payload.id).orElse(null)?.let {
            throw DuplicateSubwayStationException()
        }
        nameRepository.findByName(payload.name) ?: nameRepository.save(SubwayStation(name = payload.name, emptyList()))
        return stationRepository.save(
            SubwayRouteStation(
                id = payload.id,
                routeID = payload.routeID,
                name = payload.name,
                order = payload.order,
                cumulativeTime = LocalDateTimeBuilder.convertStringToDuration(payload.cumulativeTime),
                route = null,
                stationName = null,
                realtime = emptyList(),
                timetable = emptyList(),
            ),
        )
    }

    fun getStationById(id: String): SubwayRouteStation = stationRepository.findById(id).orElseThrow { SubwayStationNotFoundException() }

    fun cleanUpUselessStationName(name: String) {
        nameRepository.findByName(name)!!.let { station ->
            if (station.subwayLine.isEmpty()) {
                nameRepository.delete(station)
            }
        }
    }

    fun updateStation(
        id: String,
        payload: UpdateSubwayStationRequest,
    ): SubwayRouteStation {
        val station = stationRepository.findById(id).orElseThrow { SubwayStationNotFoundException() }
        if (!LocalDateTimeBuilder.checkLocalTimeFormat(payload.cumulativeTime)) {
            throw DurationNotValidException()
        }
        station.let {
            it.routeID = payload.routeID
            it.name = payload.name
            it.order = payload.order
            it.cumulativeTime = LocalDateTimeBuilder.convertStringToDuration(payload.cumulativeTime)
            return stationRepository.save(it)
        }
    }

    fun deleteStation(id: String) {
        val station = stationRepository.findById(id).orElseThrow { SubwayStationNotFoundException() }
        realtimeRepository.deleteAll(station.realtime ?: emptyList())
        timetableRepository.deleteAll(station.timetable ?: emptyList())
        stationRepository.delete(station)
        cleanUpUselessStationName(station.name)
    }

    fun getAllTimetables() = timetableRepository.findAll()

    fun getTimetablesByStationID(stationID: String) = timetableRepository.findByStationID(stationID)

    fun getTimetablesByStationIDAndDirection(
        stationID: String,
        direction: String,
    ) = timetableRepository.findByStationIDAndHeading(stationID, direction)

    fun getTimetablesByStationIDAndWeekday(
        stationID: String,
        weekday: String,
    ) = timetableRepository.findByStationIDAndWeekday(stationID, weekday)

    fun getTimetablesByStationIDAndDirectionAndWeekday(
        stationID: String,
        direction: String,
        weekday: String,
    ) = timetableRepository.findByStationIDAndHeadingAndWeekday(stationID, direction, weekday)

    fun getTimetableByStationIDAndSeq(
        stationID: String,
        seq: Int,
    ): SubwayTimetable =
        timetableRepository.findById(seq).orElseThrow { SubwayTimetableNotFoundException() }.let {
            if (it.stationID != stationID) {
                throw SubwayTimetableNotFoundException()
            }
            it
        }

    fun createTimetable(
        stationID: String,
        payload: SubwayTimetableRequest,
    ): SubwayTimetable {
        if (!LocalDateTimeBuilder.checkLocalTimeFormat(payload.departureTime)) {
            throw LocalTimeNotValidException()
        }
        stationRepository.findById(stationID).orElseThrow { SubwayStationNotFoundException() }
        stationRepository.findById(payload.startStationID).orElseThrow { SubwayStartStationNotFoundException() }
        stationRepository.findById(payload.terminalStationID).orElseThrow { SubwayTerminalStationNotFoundException() }
        timetableRepository
            .findByStationIDAndHeadingAndWeekdayAndDepartureTime(
                stationID = stationID,
                heading = payload.direction,
                weekday = payload.weekday,
                departureTime = LocalTime.parse(payload.departureTime),
            )?.let {
                throw DuplicateSubwayTimetableException()
            }
        return timetableRepository.save(
            SubwayTimetable(
                stationID = stationID,
                startStationID = payload.startStationID,
                terminalStationID = payload.terminalStationID,
                departureTime = LocalTime.parse(payload.departureTime),
                weekday = payload.weekday,
                heading = payload.direction,
                station = null,
                startStation = null,
                terminalStation = null,
            ),
        )
    }

    fun updateTimetable(
        stationID: String,
        seq: Int,
        payload: SubwayTimetableRequest,
    ): SubwayTimetable {
        if (!LocalDateTimeBuilder.checkLocalTimeFormat(payload.departureTime)) {
            throw LocalTimeNotValidException()
        }
        val timetable = timetableRepository.findById(seq).orElseThrow { SubwayTimetableNotFoundException() }
        stationRepository.findById(stationID).orElseThrow { SubwayStationNotFoundException() }
        stationRepository.findById(payload.startStationID).orElseThrow { SubwayStartStationNotFoundException() }
        stationRepository.findById(payload.terminalStationID).orElseThrow { SubwayTerminalStationNotFoundException() }
        timetable.let {
            it.stationID = stationID
            it.startStationID = payload.startStationID
            it.terminalStationID = payload.terminalStationID
            it.departureTime = LocalTime.parse(payload.departureTime)
            it.weekday = payload.weekday
            it.heading = payload.direction
            return timetableRepository.save(it)
        }
    }

    fun deleteTimetable(
        stationID: String,
        seq: Int,
    ) {
        stationRepository.findById(stationID).orElseThrow { SubwayStationNotFoundException() }
        val timetable = timetableRepository.findById(seq).orElseThrow { SubwayTimetableNotFoundException() }
        if (timetable.stationID != stationID) {
            throw SubwayTimetableNotFoundException()
        }
        timetableRepository.delete(timetable)
    }

    fun getRealtimeList() = realtimeRepository.findAll()
}
