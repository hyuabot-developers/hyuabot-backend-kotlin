package app.hyuabot.backend.bus.service

import app.hyuabot.backend.bus.domain.CreateBusStopRequest
import app.hyuabot.backend.bus.domain.UpdateBusStopRequest
import app.hyuabot.backend.bus.exception.BusStopNotFoundException
import app.hyuabot.backend.bus.exception.DuplicateBusStopException
import app.hyuabot.backend.database.entity.BusStop
import app.hyuabot.backend.database.repository.BusStopRepository
import org.springframework.stereotype.Service

@Service
class BusStopService(
    private val stopRepository: BusStopRepository,
) {
    fun getBusStopList(): List<BusStop> = stopRepository.findAll()

    fun createBusStop(payload: CreateBusStopRequest): BusStop {
        stopRepository.findById(payload.id).orElse(null)?.let {
            throw DuplicateBusStopException()
        }
        return stopRepository.save(
            BusStop(
                id = payload.id,
                name = payload.name,
                districtCode = payload.districtCode,
                mobileNumber = payload.mobileNumber,
                regionName = payload.regionName,
                latitude = payload.latitude,
                longitude = payload.longitude,
                busRoutes = emptyList(),
                startBusRoutes = emptyList(),
            ),
        )
    }

    fun getBusStopById(id: Int): BusStop = stopRepository.findById(id).orElseThrow { BusStopNotFoundException() }

    fun updateBusStop(
        id: Int,
        payload: UpdateBusStopRequest,
    ): BusStop {
        val busStop = stopRepository.findById(id).orElseThrow { BusStopNotFoundException() }
        val updatedBusStop =
            busStop.copy(
                name = payload.name,
                districtCode = payload.districtCode,
                mobileNumber = payload.mobileNumber,
                regionName = payload.regionName,
                latitude = payload.latitude,
                longitude = payload.longitude,
            )
        return stopRepository.save(updatedBusStop)
    }

    fun deleteBusStopById(id: Int) {
        stopRepository.findById(id).orElseThrow { BusStopNotFoundException() }.let { stop ->
            stopRepository.delete(stop)
        }
    }
}
