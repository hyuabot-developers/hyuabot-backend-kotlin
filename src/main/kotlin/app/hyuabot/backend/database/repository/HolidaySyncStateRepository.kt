package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.HolidaySyncState
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class HolidaySyncStateRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun findBySource(source: String): HolidaySyncState? =
        jdbcTemplate
            .query(
                """
                SELECT source, last_attempt_at, last_success_at, range_start, range_end, last_error
                FROM holiday_sync_state
                WHERE source = ?
                """.trimIndent(),
                { resultSet, _ ->
                    HolidaySyncState(
                        source = resultSet.getString("source"),
                        lastAttemptAt = resultSet.getObject("last_attempt_at", java.time.OffsetDateTime::class.java)?.toZonedDateTime(),
                        lastSuccessAt = resultSet.getObject("last_success_at", java.time.OffsetDateTime::class.java)?.toZonedDateTime(),
                        rangeStart = resultSet.getObject("range_start", java.time.LocalDate::class.java),
                        rangeEnd = resultSet.getObject("range_end", java.time.LocalDate::class.java),
                        lastError = resultSet.getString("last_error"),
                    )
                },
                source,
            ).firstOrNull()
}
