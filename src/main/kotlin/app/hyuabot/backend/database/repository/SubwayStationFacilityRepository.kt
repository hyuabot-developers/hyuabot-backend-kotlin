package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.SubwayStationFacility
import org.springframework.data.jpa.repository.JpaRepository

interface SubwayStationFacilityRepository : JpaRepository<SubwayStationFacility, Int> {
    fun findByStationIDOrderByFacilityTypeAscSortOrderAsc(stationID: String): List<SubwayStationFacility>
}
