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

    fun getBusRealtimeBatch(keys: Set<Pair<Int, Int>>): Map<Pair<Int, Int>, List<BusRealtime>> {
        if (keys.isEmpty()) return emptyMap()
        val routeIDs = keys.map { it.first }.distinct()
        val stopIDs = keys.map { it.second }.distinct()
        val grouped = realtimeRepository.findByRouteIDInAndStopIDIn(routeIDs, stopIDs)
            .groupBy { it.routeID to it.stopID }
        return keys.associateWith { key -> grouped[key] ?: emptyList() }
    }
}
