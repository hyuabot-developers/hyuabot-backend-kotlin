package app.hyuabot.backend.holiday

import app.hyuabot.backend.database.entity.HolidaySyncState
import app.hyuabot.backend.database.repository.HolidaySyncStateRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.time.LocalDate
import java.time.OffsetDateTime

class HolidaySyncStateRepositoryTest {
    private val jdbcTemplate = mock<JdbcTemplate>()
    private val repository = HolidaySyncStateRepository(jdbcTemplate)

    @Test
    fun `sync state row is mapped without registering a JPA entity`() {
        val resultSet = mock<ResultSet>()
        val success = OffsetDateTime.parse("2026-07-17T03:10:00+09:00")
        whenever(resultSet.getString("source")).thenReturn("KASI")
        whenever(resultSet.getObject("last_attempt_at", OffsetDateTime::class.java)).thenReturn(success)
        whenever(resultSet.getObject("last_success_at", OffsetDateTime::class.java)).thenReturn(success)
        whenever(resultSet.getObject("range_start", LocalDate::class.java)).thenReturn(LocalDate.of(2026, 1, 1))
        whenever(resultSet.getObject("range_end", LocalDate::class.java)).thenReturn(LocalDate.of(2027, 12, 31))
        whenever(resultSet.getString("last_error")).thenReturn(null)
        whenever(jdbcTemplate.query(any<String>(), any<RowMapper<HolidaySyncState>>(), eq("KASI"))).thenAnswer { invocation ->
            val mapper = invocation.getArgument<RowMapper<HolidaySyncState>>(1)
            listOf(mapper.mapRow(resultSet, 0))
        }

        val result = repository.findBySource("KASI")

        assertEquals(success.toZonedDateTime(), result?.lastSuccessAt)
        assertEquals(LocalDate.of(2027, 12, 31), result?.rangeEnd)
        assertNull(result?.lastError)
    }

    @Test
    fun `missing sync state returns null`() {
        whenever(jdbcTemplate.query(any<String>(), any<RowMapper<HolidaySyncState>>(), eq("KASI"))).thenReturn(emptyList())

        assertNull(repository.findBySource("KASI"))
    }
}
