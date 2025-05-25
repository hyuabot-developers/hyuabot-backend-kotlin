package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.Room
import app.hyuabot.backend.database.key.RoomID
import org.springframework.data.jpa.repository.JpaRepository

interface RoomRepository : JpaRepository<Room, RoomID> {
    // Single Query
    fun findByBuildingNameAndNumber(
        buildingName: String,
        number: String,
    ): Room

    fun deleteByNameAndNumber(
        buildingName: String,
        number: String,
    ): Int

    // List Query
    fun findByNameContaining(buildingName: String): List<Room>
}
