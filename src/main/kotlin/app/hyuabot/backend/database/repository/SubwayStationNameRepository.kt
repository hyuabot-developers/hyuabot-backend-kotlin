package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.SubwayStation
import org.springframework.data.jpa.repository.JpaRepository

interface SubwayStationNameRepository : JpaRepository<SubwayStation, String> {
    fun findByNameContaining(name: String): List<SubwayStation>
}
