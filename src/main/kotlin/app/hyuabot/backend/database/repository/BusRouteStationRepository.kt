package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.BusRouteStation
import app.hyuabot.backend.database.key.BusRouteStationID
import org.springframework.data.jpa.repository.JpaRepository

interface BusRouteStationRepository : JpaRepository<BusRouteStation, BusRouteStationID> {
    fun findByRouteIDOrderByStationSeqAsc(routeID: Int): List<BusRouteStation>
}
