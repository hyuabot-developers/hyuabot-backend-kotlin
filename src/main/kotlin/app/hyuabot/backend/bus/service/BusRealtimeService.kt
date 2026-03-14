package app.hyuabot.backend.bus.service

import app.hyuabot.backend.database.entity.BusRealtime
import app.hyuabot.backend.database.repository.BusRealtimeRepository
import org.springframework.stereotype.Service

@Service
class BusRealtimeService(
    private val realtimeRepository: BusRealtimeRepository,
) {
    fun getBusRealtimeList(): List<BusRealtime> = realtimeRepository.findAll()

    fun getBusRealtimeListByBusStop(
        routeID: Int,
        stopID: Int,
    ): List<BusRealtime> = realtimeRepository.findByRouteIDAndStopID(routeID, stopID)
}
