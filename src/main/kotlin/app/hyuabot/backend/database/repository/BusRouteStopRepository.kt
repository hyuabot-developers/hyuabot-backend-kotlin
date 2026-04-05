package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.BusRouteStop
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BusRouteStopRepository : JpaRepository<BusRouteStop, Int> {
    fun findByRouteID(routeID: Int): List<BusRouteStop>

    fun findByStopID(stopID: Int): List<BusRouteStop>

    fun findByRouteIDAndOrder(
        routeID: Int,
        order: Int,
    ): BusRouteStop?

    fun findByRouteIDAndOrderAndSeqNot(
        routeID: Int,
        order: Int,
        seq: Int,
    ): BusRouteStop?

    fun findByRouteIDAndSeq(
        routeID: Int,
        seq: Int,
    ): BusRouteStop?

    @Query(
        """
                    SELECT rs FROM bus_route_stop rs
        JOIN FETCH rs.route r
        JOIN FETCH rs.stop s
        JOIN FETCH rs.startStop ss
        WHERE rs.routeID in :routes AND rs.stopID in :stops
        """,
    )
    fun fetchBusRouteStops(
        @Param("routes") routes: List<Int>,
        @Param("stops") stops: List<Int>,
    ): List<BusRouteStop>
}
