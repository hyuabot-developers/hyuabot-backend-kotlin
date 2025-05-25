package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.ShuttleStop
import org.springframework.data.jpa.repository.JpaRepository

interface ShuttleStopRepository : JpaRepository<ShuttleStop, String> {
    fun findByNameContaining(name: String): List<ShuttleStop>
}
