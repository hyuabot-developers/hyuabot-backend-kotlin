package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.Cafeteria
import org.springframework.data.jpa.repository.JpaRepository

interface CafeteriaRepository : JpaRepository<Cafeteria, Int> {
    fun findByNameContaining(name: String): List<Cafeteria>

    fun findByCampusID(campusID: Int): List<Cafeteria>
}
