package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.BusRoute
import org.springframework.data.jpa.repository.JpaRepository

interface BusRouteRepository : JpaRepository<BusRoute, Int> {
    fun findByNameContaining(name: String): List<BusRoute>
}
