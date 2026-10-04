package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.BusLocation
import app.hyuabot.backend.database.key.BusLocationID
import org.springframework.data.jpa.repository.JpaRepository

interface BusLocationRepository : JpaRepository<BusLocation, BusLocationID> {
    fun findByRouteIDOrderByStationSeqDesc(routeID: Int): List<BusLocation>
}
