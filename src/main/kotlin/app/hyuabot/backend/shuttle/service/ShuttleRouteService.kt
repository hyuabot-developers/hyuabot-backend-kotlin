package app.hyuabot.backend.shuttle.service

import app.hyuabot.backend.database.entity.ShuttleRoute
import app.hyuabot.backend.database.entity.ShuttleRouteStop
import app.hyuabot.backend.database.entity.ShuttleTimetable
import app.hyuabot.backend.database.exception.DurationNotValidException
import app.hyuabot.backend.database.exception.LocalTimeNotValidException
import app.hyuabot.backend.database.repository.ShuttleRouteRepository
import app.hyuabot.backend.database.repository.ShuttleRouteStopRepository
import app.hyuabot.backend.database.repository.ShuttleStopRepository
import app.hyuabot.backend.database.repository.ShuttleTimetableRepository
import app.hyuabot.backend.shuttle.domain.CreateShuttleRouteRequest
import app.hyuabot.backend.shuttle.domain.CreateShuttleRouteStopRequest
import app.hyuabot.backend.shuttle.domain.ShuttleTimetableRequest
import app.hyuabot.backend.shuttle.domain.UpdateShuttleRouteRequest
import app.hyuabot.backend.shuttle.domain.UpdateShuttleRouteStopRequest
import app.hyuabot.backend.shuttle.exception.DuplicateShuttleRouteStopException
import app.hyuabot.backend.shuttle.exception.DuplicateShuttleStopException
import app.hyuabot.backend.shuttle.exception.DuplicateShuttleTimetableException
import app.hyuabot.backend.shuttle.exception.ShuttleRouteNotFoundException
import app.hyuabot.backend.shuttle.exception.ShuttleRouteStopNotFoundException
import app.hyuabot.backend.shuttle.exception.ShuttleTimetableNotFoundException
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import org.springframework.stereotype.Service
import java.time.LocalTime

@Service
class ShuttleRouteService(
    private val shuttleRouteRepository: ShuttleRouteRepository,
    private val shuttleStopRepository: ShuttleStopRepository,
    private val shuttleRouteStopRepository: ShuttleRouteStopRepository,
    private val shuttleTimetableRepository: ShuttleTimetableRepository,
) {
    fun getAllRoutes(name: String? = null) =
        if (!name.isNullOrEmpty()) {
            shuttleRouteRepository.findByNameContaining(name)
        } else {
            shuttleRouteRepository.findAll()
        }.sortedBy { it.name }

    fun getRouteByName(name: String): ShuttleRoute =
        shuttleRouteRepository.findById(name).orElseThrow {
            ShuttleRouteNotFoundException()
        }

    fun createRoute(payload: CreateShuttleRouteRequest): ShuttleRoute {
        if (shuttleRouteRepository.existsById(payload.name)) {
            throw DuplicateShuttleStopException()
        }
        return shuttleRouteRepository.save(
            ShuttleRoute(
                name = payload.name,
                descriptionKorean = payload.descriptionKorean,
                descriptionEnglish = payload.descriptionEnglish,
                tag = payload.tag,
                startStopID = payload.startStopID,
                endStopID = payload.endStopID,
                startStop = null,
                endStop = null,
                timetable = emptyList(),
                stop = emptyList(),
            ),
        )
    }

    fun updateRoute(
        name: String,
        payload: UpdateShuttleRouteRequest,
    ): ShuttleRoute {
        val shuttleRoute =
            shuttleRouteRepository.findById(name).orElseThrow {
                ShuttleRouteNotFoundException()
            }
        shuttleRoute.apply {
            descriptionKorean = payload.descriptionKorean
            descriptionEnglish = payload.descriptionEnglish
            tag = payload.tag
            startStopID = payload.startStopID
            endStopID = payload.endStopID
        }
        return shuttleRouteRepository.save(shuttleRoute)
    }

    fun deleteRouteByName(name: String) {
        shuttleRouteRepository.findById(name).orElseThrow { ShuttleRouteNotFoundException() }
        shuttleRouteRepository.deleteById(name)
    }

    fun getShuttleStopByRouteName(routeName: String): List<ShuttleRouteStop> {
        shuttleRouteRepository
            .findById(routeName)
            .orElseThrow { ShuttleRouteNotFoundException() }
        return shuttleRouteStopRepository.findByRouteName(routeName).sortedBy { it.order }
    }

    fun createShuttleRouteStop(
        routeName: String,
        payload: CreateShuttleRouteStopRequest,
    ): ShuttleRouteStop {
        // Check if the route and stop exist
        shuttleRouteRepository.findById(routeName).orElseThrow { ShuttleRouteNotFoundException() }
        shuttleStopRepository.findById(payload.stopName).orElseThrow { ShuttleRouteNotFoundException() }
        shuttleRouteStopRepository
            .findByRouteNameAndStopName(
                routeName = routeName,
                stopName = payload.stopName,
            )?.let { throw DuplicateShuttleRouteStopException() }
        // Check cumulative time has valid format (HH:mm:ss)
        payload.cumulativeTime.let {
            if (!it.matches(Regex("\\d{2}:\\d{2}:\\d{2}"))) {
                throw DurationNotValidException()
            }
        }

        return shuttleRouteStopRepository.save(
            ShuttleRouteStop(
                routeName = routeName,
                stopName = payload.stopName,
                order = payload.order,
                cumulativeTime = LocalDateTimeBuilder.convertStringToDuration(payload.cumulativeTime),
                route = null,
                stop = null,
            ),
        )
    }

    fun updateShuttleRouteStop(
        routeName: String,
        stopName: String,
        payload: UpdateShuttleRouteStopRequest,
    ): ShuttleRouteStop {
        shuttleRouteRepository.findById(routeName).orElseThrow { throw ShuttleRouteNotFoundException() }
        val shuttleRouteStop =
            shuttleRouteStopRepository.findByRouteNameAndStopName(routeName, stopName)
                ?: throw ShuttleRouteStopNotFoundException()

        // Check cumulative time has valid format (HH:mm:ss)
        payload.cumulativeTime.let {
            if (!it.matches(Regex("\\d{2}:\\d{2}:\\d{2}"))) {
                throw DurationNotValidException()
            }
        }
        shuttleRouteStop.apply {
            order = payload.order
            cumulativeTime = LocalDateTimeBuilder.convertStringToDuration(payload.cumulativeTime)
        }
        return shuttleRouteStopRepository.save(shuttleRouteStop)
    }

    fun deleteShuttleRouteStop(
        routeName: String,
        stopName: String,
    ) {
        shuttleRouteStopRepository.findByRouteNameAndStopName(routeName, stopName)?.let {
            shuttleRouteStopRepository.delete(it)
        } ?: throw ShuttleRouteStopNotFoundException()
    }

    fun getShuttleTimetableByRouteName(
        routeName: String,
        period: String?,
        isWeekdays: Boolean?,
    ): List<ShuttleTimetable> {
        shuttleRouteRepository.findById(routeName).orElseThrow { ShuttleRouteNotFoundException() }
        return when {
            period != null && isWeekdays != null -> {
                shuttleTimetableRepository.findByPeriodTypeAndRouteNameAndWeekday(
                    routeName = routeName,
                    periodType = period,
                    isWeekdays = isWeekdays,
                )
            }
            period != null -> {
                shuttleTimetableRepository.findByPeriodTypeAndRouteName(
                    routeName = routeName,
                    periodType = period,
                )
            }
            isWeekdays != null -> {
                shuttleTimetableRepository.findByRouteNameAndWeekday(
                    routeName = routeName,
                    isWeekdays = isWeekdays,
                )
            }
            else -> {
                shuttleTimetableRepository.findByRouteName(routeName)
            }
        }
    }

    fun createShuttleTimetable(
        routeName: String,
        payload: ShuttleTimetableRequest,
    ): ShuttleTimetable {
        shuttleRouteRepository.findById(routeName).orElseThrow { ShuttleRouteNotFoundException() }
        // Check if the departure time is in valid format (HH:mm:ss)
        payload.departureTime.let {
            if (!it.matches(Regex("\\d{2}:\\d{2}:\\d{2}"))) {
                throw LocalTimeNotValidException()
            }
        }
        shuttleTimetableRepository
            .findByRouteNameAndPeriodTypeAndWeekdayAndDepartureTime(
                routeName = routeName,
                periodType = payload.periodType,
                isWeekdays = payload.weekday,
                departureTime = LocalTime.parse(payload.departureTime),
            )?.let {
                throw DuplicateShuttleTimetableException()
            }

        return shuttleTimetableRepository.save(
            ShuttleTimetable(
                seq = null,
                routeName = routeName,
                periodType = payload.periodType,
                weekday = payload.weekday,
                departureTime = LocalTime.parse(payload.departureTime),
                route = null,
            ),
        )
    }

    fun getShuttleTimetableByRouteNameAndSeq(
        routeName: String,
        seq: Int,
    ): ShuttleTimetable {
        shuttleRouteRepository.findById(routeName).orElseThrow { ShuttleRouteNotFoundException() }
        return shuttleTimetableRepository.findByRouteNameAndSeq(routeName, seq)
            ?: throw ShuttleTimetableNotFoundException()
    }

    fun updateShuttleTimetable(
        routeName: String,
        seq: Int,
        payload: ShuttleTimetableRequest,
    ): ShuttleTimetable {
        shuttleRouteRepository.findById(routeName).orElseThrow { ShuttleRouteNotFoundException() }
        val shuttleTimetable =
            shuttleTimetableRepository.findByRouteNameAndSeq(routeName, seq)
                ?: throw ShuttleTimetableNotFoundException()

        // Check if the departure time is in valid format (HH:mm:ss)
        payload.departureTime.let {
            if (!it.matches(Regex("\\d{2}:\\d{2}:\\d{2}"))) {
                throw LocalTimeNotValidException()
            }
        }
        shuttleTimetable.apply {
            periodType = payload.periodType
            weekday = payload.weekday
            departureTime = LocalTime.parse(payload.departureTime)
        }
        return shuttleTimetableRepository.save(shuttleTimetable)
    }

    fun deleteShuttleTimetable(
        routeName: String,
        seq: Int,
    ) {
        shuttleRouteRepository.findById(routeName).orElseThrow { ShuttleRouteNotFoundException() }
        shuttleTimetableRepository.findById(seq).orElseThrow { ShuttleTimetableNotFoundException() }
        shuttleTimetableRepository.findByRouteNameAndSeq(routeName, seq)?.let {
            shuttleTimetableRepository.delete(it)
        } ?: throw ShuttleTimetableNotFoundException()
    }
}
