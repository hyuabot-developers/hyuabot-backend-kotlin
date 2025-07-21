package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.Campus
import org.springframework.data.jpa.repository.JpaRepository

interface CampusRepository : JpaRepository<Campus, Int> {
    fun findByNameContaining(name: String): List<Campus>

    fun findByName(name: String): List<Campus>
}
