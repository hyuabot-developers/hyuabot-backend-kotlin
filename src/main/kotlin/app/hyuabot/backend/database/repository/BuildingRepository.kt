package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.Building
import org.springframework.data.jpa.repository.JpaRepository

interface BuildingRepository : JpaRepository<Building, String> {
    // Single Query
    fun findByName(name: String): Building?

    fun deleteBuildingByName(name: String): Int

    // List Query
    fun findByNameContaining(name: String): List<Building>

    fun findByCampusIDAndNameContaining(
        campusID: Int,
        name: String,
    ): List<Building>

    fun findByLatitudeBetweenAndLongitudeBetween(
        south: Double,
        north: Double,
        west: Double,
        east: Double,
    ): List<Building>
}
