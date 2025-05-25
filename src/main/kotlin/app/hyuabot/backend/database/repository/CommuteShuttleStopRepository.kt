package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.CommuteShuttleStop
import org.springframework.data.jpa.repository.JpaRepository

interface CommuteShuttleStopRepository : JpaRepository<CommuteShuttleStop, String> {
    fun findByNameContaining(name: String): List<CommuteShuttleStop>
}
