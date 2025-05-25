package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.SubwayRealtime
import app.hyuabot.backend.database.key.SubwayRealtimeID
import org.springframework.data.jpa.repository.JpaRepository

interface SubwayRealtimeRepository : JpaRepository<SubwayRealtime, SubwayRealtimeID> {
    fun findByStationID(stationID: String): List<SubwayRealtime>

    fun findByStationIDAndHeading(
        stationID: String,
        heading: String,
    ): List<SubwayRealtime>
}
