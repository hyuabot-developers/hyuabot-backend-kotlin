package app.hyuabot.backend.database.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class SubwayStationTranslationRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun findName(
        stationID: String,
        language: String,
    ): String? =
        jdbcTemplate
            .queryForList(
                """
                SELECT name
                FROM subway_station_translation
                WHERE station_id = ? AND language = ?
                """.trimIndent(),
                String::class.java,
                stationID,
                language,
            ).firstOrNull()
}
