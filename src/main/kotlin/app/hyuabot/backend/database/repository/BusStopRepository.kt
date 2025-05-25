package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.BusStop
import org.springframework.data.jpa.repository.JpaRepository

interface BusStopRepository : JpaRepository<BusStop, Int> {
    fun findByNameContaining(name: String): List<BusStop>
}
