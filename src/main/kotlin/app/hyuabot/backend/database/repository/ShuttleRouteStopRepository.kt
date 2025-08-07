package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.ShuttleRouteStop
import org.springframework.data.jpa.repository.JpaRepository

interface ShuttleRouteStopRepository : JpaRepository<ShuttleRouteStop, Int> {
    fun findByRouteName(routeName: String): List<ShuttleRouteStop>

    fun findByStopName(stopName: String): List<ShuttleRouteStop>

    fun findByRouteNameAndStopName(
        routeName: String,
        stopName: String,
    ): ShuttleRouteStop?
}
