package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.SubwayRouteStation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SubwayStationRepository : JpaRepository<SubwayRouteStation, String> {
    fun findByNameContaining(name: String): List<SubwayRouteStation>

    @Query(
        """
            select s FROM subway_route_station s
            JOIN FETCH s.route r
            WHERE s.id IN :ids
        """,
    )
    fun findByIdIn(ids: List<String>): List<SubwayRouteStation>

    fun findByRouteID(routeID: Int): List<SubwayRouteStation>
}
