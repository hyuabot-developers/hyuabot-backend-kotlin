package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.BusRouteStop
import app.hyuabot.backend.database.key.BusRouteStopID
import org.springframework.data.jpa.repository.JpaRepository

interface BusRouteStopRepository : JpaRepository<BusRouteStop, BusRouteStopID> {
    fun findByRouteID(routeID: Int): List<BusRouteStop>

    fun findByStopID(stopID: Int): List<BusRouteStop>
}
