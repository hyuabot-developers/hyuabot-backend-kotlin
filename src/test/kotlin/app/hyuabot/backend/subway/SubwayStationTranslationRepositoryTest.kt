package app.hyuabot.backend.subway

import app.hyuabot.backend.database.repository.SubwayStationTranslationRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubwayStationTranslationRepositoryTest {
    private val jdbcTemplate = mock<JdbcTemplate>()
    private val repository = SubwayStationTranslationRepository(jdbcTemplate)

    @Test
    fun `returns translated station name`() {
        whenever(
            jdbcTemplate.queryForList(
                any<String>(),
                eq(String::class.java),
                eq("K453"),
                eq("en"),
            ),
        ).thenReturn(listOf("Ansan"))

        assertEquals("Ansan", repository.findName("K453", "en"))
    }

    @Test
    fun `returns null when translation is missing`() {
        whenever(
            jdbcTemplate.queryForList(
                any<String>(),
                eq(String::class.java),
                eq("K453"),
                eq("en"),
            ),
        ).thenReturn(emptyList())

        assertNull(repository.findName("K453", "en"))
    }
}
