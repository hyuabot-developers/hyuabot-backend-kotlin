package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.BusRealtime
import app.hyuabot.backend.database.key.BusRealtimeID
import org.springframework.data.jpa.repository.JpaRepository

interface BusRealtimeRepository : JpaRepository<BusRealtime, BusRealtimeID> {
    fun findByRouteIDAndStopID(
        routeID: Int,
        stopID: Int,
    ): List<BusRealtime>
}
