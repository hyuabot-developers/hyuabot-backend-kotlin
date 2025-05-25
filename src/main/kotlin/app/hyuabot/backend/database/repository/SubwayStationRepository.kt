package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.SubwayRouteStation
import org.springframework.data.jpa.repository.JpaRepository

interface SubwayStationRepository : JpaRepository<SubwayRouteStation, String> {
    fun findByNameContaining(name: String): List<SubwayRouteStation>

    fun findByRouteID(routeID: Int): List<SubwayRouteStation>
}
