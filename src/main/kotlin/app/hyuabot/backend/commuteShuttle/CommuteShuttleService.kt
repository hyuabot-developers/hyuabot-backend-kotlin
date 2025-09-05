package app.hyuabot.backend.commuteShuttle

import app.hyuabot.backend.commuteShuttle.domain.CreateShuttleRouteRequest
import app.hyuabot.backend.commuteShuttle.domain.CreateShuttleStopRequest
import app.hyuabot.backend.commuteShuttle.domain.ShuttleTimetableRequest
import app.hyuabot.backend.commuteShuttle.domain.UpdateShuttleRouteRequest
import app.hyuabot.backend.commuteShuttle.domain.UpdateShuttleStopRequest
import app.hyuabot.backend.commuteShuttle.exception.DuplicateShuttleRouteException
import app.hyuabot.backend.commuteShuttle.exception.DuplicateShuttleStopException
import app.hyuabot.backend.commuteShuttle.exception.ShuttleRouteNotFoundException
import app.hyuabot.backend.commuteShuttle.exception.ShuttleStopNotFoundException
import app.hyuabot.backend.commuteShuttle.exception.ShuttleTimetableNotFoundException
import app.hyuabot.backend.database.entity.CommuteShuttleRoute
import app.hyuabot.backend.database.entity.CommuteShuttleStop
import app.hyuabot.backend.database.entity.CommuteShuttleTimetable
import app.hyuabot.backend.database.exception.LocalTimeNotValidException
import app.hyuabot.backend.database.repository.CommuteShuttleRouteRepository
import app.hyuabot.backend.database.repository.CommuteShuttleStopRepository
import app.hyuabot.backend.database.repository.CommuteShuttleTimetableRepository
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import org.springframework.stereotype.Service
import java.time.LocalTime

@Service
class CommuteShuttleService(
    private val routeRepository: CommuteShuttleRouteRepository,
    private val stopRepository: CommuteShuttleStopRepository,
    private val timetableRepository: CommuteShuttleTimetableRepository,
) {
    fun getAllRoutes() = routeRepository.findAll()

    fun getRouteByName(name: String) = routeRepository.findByName(name) ?: throw ShuttleRouteNotFoundException()

    fun createRoute(payload: CreateShuttleRouteRequest): CommuteShuttleRoute {
        routeRepository.findByName(payload.name)?.let {
            throw DuplicateShuttleRouteException()
        }
        return routeRepository.save(
            CommuteShuttleRoute(
                name = payload.name,
                descriptionKorean = payload.descriptionKorean,
                descriptionEnglish = payload.descriptionEnglish,
                timetable = emptyList(),
            ),
        )
    }

    fun updateRoute(
        name: String,
        payload: UpdateShuttleRouteRequest,
    ): CommuteShuttleRoute {
        val route = routeRepository.findById(name).orElseThrow { ShuttleRouteNotFoundException() }
        route.let {
            it.descriptionKorean = payload.descriptionKorean
            it.descriptionEnglish = payload.descriptionEnglish
            return routeRepository.save(it)
        }
    }

    fun deleteRoute(name: String) {
        val route = routeRepository.findById(name).orElseThrow { ShuttleRouteNotFoundException() }
        val timetables = timetableRepository.findByRouteName(name)
        timetableRepository.deleteAll(timetables)
        routeRepository.delete(route)
    }

    fun getAllStops() = stopRepository.findAll()

    fun getStopByName(name: String) = stopRepository.findByName(name) ?: throw ShuttleStopNotFoundException()

    fun createStop(payload: CreateShuttleStopRequest): CommuteShuttleStop {
        stopRepository.findByName(payload.name)?.let {
            throw DuplicateShuttleStopException()
        }
        return stopRepository.save(
            CommuteShuttleStop(
                name = payload.name,
                description = payload.description,
                latitude = payload.latitude,
                longitude = payload.longitude,
                timetable = emptyList(),
            ),
        )
    }

    fun updateStop(
        name: String,
        payload: UpdateShuttleStopRequest,
    ): CommuteShuttleStop {
        val stop = stopRepository.findById(name).orElseThrow { ShuttleStopNotFoundException() }
        stop.let {
            it.description = payload.description
            it.latitude = payload.latitude
            it.longitude = payload.longitude
            return stopRepository.save(it)
        }
    }

    fun deleteStop(name: String) {
        val stop = stopRepository.findById(name).orElseThrow { ShuttleStopNotFoundException() }
        val timetables = timetableRepository.findByStopName(name)
        timetableRepository.deleteAll(timetables)
        stopRepository.delete(stop)
    }

    fun getAllTimetables() = timetableRepository.findAll()

    fun getTimetableByRouteName(routeName: String) = timetableRepository.findByRouteName(routeName)

    fun getShuttleTimetableByRouteNameAndSeq(
        routeName: String,
        seq: Int,
    ) = timetableRepository.findByRouteNameAndSeq(routeName, seq) ?: throw ShuttleTimetableNotFoundException()

    fun createTimetable(
        routeName: String,
        payload: ShuttleTimetableRequest,
    ): CommuteShuttleTimetable {
        routeRepository.findById(routeName).orElseThrow { ShuttleRouteNotFoundException() }
        stopRepository.findById(payload.stopID).orElseThrow { ShuttleStopNotFoundException() }
        if (!LocalDateTimeBuilder.checkLocalTimeFormat(payload.time)) {
            throw LocalTimeNotValidException()
        }
        val timetable =
            CommuteShuttleTimetable(
                routeName = routeName,
                stopName = payload.stopID,
                order = payload.order,
                departureTime = LocalTime.parse(payload.time),
                route = null,
                stop = null,
            )
        return timetableRepository.save(timetable)
    }

    fun updateTimetable(
        routeName: String,
        seq: Int,
        payload: ShuttleTimetableRequest,
    ): CommuteShuttleTimetable {
        val timetable = timetableRepository.findById(seq).orElseThrow { ShuttleTimetableNotFoundException() }
        routeRepository.findById(routeName).orElseThrow { ShuttleRouteNotFoundException() }
        stopRepository.findById(payload.stopID).orElseThrow { ShuttleStopNotFoundException() }
        if (!LocalDateTimeBuilder.checkLocalTimeFormat(payload.time)) {
            throw LocalTimeNotValidException()
        }
        timetable.let {
            it.routeName = payload.routeName
            it.stopName = payload.stopID
            it.order = payload.order
            it.departureTime = LocalTime.parse(payload.time)
            return timetableRepository.save(it)
        }
    }

    fun deleteTimetable(seq: Int) {
        val timetable = timetableRepository.findById(seq).orElseThrow { ShuttleTimetableNotFoundException() }
        timetableRepository.delete(timetable)
    }
}
