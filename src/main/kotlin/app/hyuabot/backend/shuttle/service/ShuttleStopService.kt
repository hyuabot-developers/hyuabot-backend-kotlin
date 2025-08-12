package app.hyuabot.backend.shuttle.service

import app.hyuabot.backend.database.entity.ShuttleStop
import app.hyuabot.backend.database.repository.ShuttleStopRepository
import app.hyuabot.backend.shuttle.domain.CreateShuttleStopRequest
import app.hyuabot.backend.shuttle.domain.UpdateShuttleStopRequest
import app.hyuabot.backend.shuttle.exception.DuplicateShuttleStopException
import app.hyuabot.backend.shuttle.exception.ShuttleStopNotFoundException
import org.springframework.stereotype.Service

@Service
class ShuttleStopService(
    private val shuttleStopRepository: ShuttleStopRepository,
) {
    fun getAllStops(name: String? = null): List<ShuttleStop> =
        if (!name.isNullOrBlank()) {
            shuttleStopRepository.findByNameContaining(name)
        } else {
            shuttleStopRepository.findAll()
        }.sortedBy { it.name }

    fun getStopByName(name: String): ShuttleStop =
        shuttleStopRepository
            .findById(name)
            .orElseThrow { ShuttleStopNotFoundException() }

    fun createStop(payload: CreateShuttleStopRequest): ShuttleStop {
        shuttleStopRepository.findByName(payload.name)?.let {
            throw DuplicateShuttleStopException()
        }
        return shuttleStopRepository.save(
            ShuttleStop(
                name = payload.name,
                latitude = payload.latitude,
                longitude = payload.longitude,
                route = emptyList(),
                routeToStart = emptyList(),
                routeToEnd = emptyList(),
            ),
        )
    }

    fun updateStop(
        name: String,
        payload: UpdateShuttleStopRequest,
    ): ShuttleStop {
        shuttleStopRepository.findById(name).orElseThrow { ShuttleStopNotFoundException() }.let { shuttleStop ->
            shuttleStop.apply {
                latitude = payload.latitude
                longitude = payload.longitude
            }
            return shuttleStopRepository.save(shuttleStop)
        }
    }

    fun deleteStopByName(name: String) {
        shuttleStopRepository.findByName(name)?.let { shuttleStop ->
            shuttleStopRepository.delete(shuttleStop)
        } ?: throw ShuttleStopNotFoundException()
    }
}
