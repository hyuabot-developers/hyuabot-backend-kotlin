package app.hyuabot.backend.shuttle

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
    fun getAllStops(name: String): List<ShuttleStop> =
        if (name.isNotEmpty()) {
            shuttleStopRepository.findByNameContaining(name)
        } else {
            shuttleStopRepository.findAll()
        }.sortedBy { it.name }

    fun getStopByName(name: String): ShuttleStop =
        shuttleStopRepository
            .findById(name)
            .orElseThrow { ShuttleStopNotFoundException() }

    fun createStop(payload: CreateShuttleStopRequest): ShuttleStop {
        shuttleStopRepository.findById(payload.name).let {
            DuplicateShuttleStopException()
        }
        return shuttleStopRepository.save(
            ShuttleStop(
                name = payload.name,
                latitude = payload.latitude,
                longitude = payload.longitude,
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
        shuttleStopRepository.findById(name).orElseThrow { ShuttleStopNotFoundException() }
        shuttleStopRepository.deleteById(name)
    }
}
