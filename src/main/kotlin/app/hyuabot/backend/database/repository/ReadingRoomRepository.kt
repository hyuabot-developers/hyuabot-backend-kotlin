package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.ReadingRoom
import org.springframework.data.jpa.repository.JpaRepository

interface ReadingRoomRepository : JpaRepository<ReadingRoom, Int> {
    fun findByNameContaining(name: String): List<ReadingRoom>

    fun findByCampusId(campusId: Int): List<ReadingRoom>

    fun findByCampusIdAndNameContaining(
        campusId: Int,
        name: String,
    ): List<ReadingRoom>
}
