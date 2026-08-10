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

    fun findNameByKoreanName(
        koreanName: String,
        language: String,
    ): String? =
        jdbcTemplate
            .queryForList(
                """
                SELECT target.name
                FROM subway_station_translation source
                JOIN subway_station_translation target ON target.station_id = source.station_id
                WHERE source.language = 'ko'
                  AND source.name = ?
                  AND target.language = ?
                ORDER BY target.station_id
                LIMIT 1
                """.trimIndent(),
                String::class.java,
                koreanName,
                language,
            ).firstOrNull()
}
