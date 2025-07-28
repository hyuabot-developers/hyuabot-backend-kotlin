package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.Room
import org.springframework.data.jpa.repository.JpaRepository

interface RoomRepository : JpaRepository<Room, Int> {
    // Single Query
    fun findByBuildingNameAndNumber(
        buildingName: String,
        number: String,
    ): Room?

    fun deleteByBuildingNameAndNumber(
        buildingName: String,
        number: String,
    ): Int

    // List Query
    fun findByNameContaining(buildingName: String): List<Room>

    fun findByBuildingName(buildingName: String): List<Room>

    fun findByBuildingNameAndNameContaining(
        buildingName: String,
        number: String,
    ): List<Room>
}
