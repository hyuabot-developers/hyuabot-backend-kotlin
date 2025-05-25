package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.SubwayRoute
import org.springframework.data.jpa.repository.JpaRepository

interface SubwayRouteRepository : JpaRepository<SubwayRoute, Int> {
    fun findByNameContaining(name: String): List<SubwayRoute>
}
