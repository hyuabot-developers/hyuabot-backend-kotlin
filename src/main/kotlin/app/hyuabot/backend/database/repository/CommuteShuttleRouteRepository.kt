package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.CommuteShuttleRoute
import org.springframework.data.jpa.repository.JpaRepository

interface CommuteShuttleRouteRepository : JpaRepository<CommuteShuttleRoute, String> {
    fun findByNameContaining(name: String): List<CommuteShuttleRoute>
}
