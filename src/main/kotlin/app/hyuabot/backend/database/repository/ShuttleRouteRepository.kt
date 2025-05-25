package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.ShuttleRoute
import org.springframework.data.jpa.repository.JpaRepository

interface ShuttleRouteRepository : JpaRepository<ShuttleRoute, String> {
    fun findByNameContaining(name: String): List<ShuttleRoute>
}
