package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.BusRouteStop
import org.springframework.data.jpa.repository.JpaRepository

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
}
