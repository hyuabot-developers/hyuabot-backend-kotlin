package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.SubwayRealtime
import app.hyuabot.backend.database.key.SubwayRealtimeID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SubwayRealtimeRepository : JpaRepository<SubwayRealtime, SubwayRealtimeID> {
    fun findByStationID(stationID: String): List<SubwayRealtime>

    @Query(
        """
            SELECT r FROM subway_realtime r
            JOIN FETCH r.terminalStation ts
            JOIN FETCH ts.route
            WHERE r.stationID = :stationID
            AND r.heading IN :directions
            ORDER BY r.order ASC
        """,
    )
    fun findByStationIDAndHeadingIn(
        stationID: String,
        directions: List<String>,
    ): List<SubwayRealtime>

    fun findByStationIDAndHeading(
        stationID: String,
        heading: String,
    ): List<SubwayRealtime>
}
