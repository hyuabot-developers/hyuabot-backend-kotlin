package app.hyuabot.backend.holiday

import app.hyuabot.backend.database.entity.PublicHoliday
import app.hyuabot.backend.holiday.service.PublicHolidayService
import app.hyuabot.backend.utility.ScalarRegistration
import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.test.EnableDgsTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.LocalDate

@EnableDgsTest
@SpringJUnitConfig(PublicHolidayDataFetcher::class)
@Import(PublicHolidayDataFetcher::class, ScalarRegistration::class)
class PublicHolidayDataFetcherTest {
    @MockitoBean
    lateinit var publicHolidayService: PublicHolidayService

    @Autowired
    lateinit var dgsQueryExecutor: DgsQueryExecutor

    @Test
    fun `returns public holidays in date range`() {
        val holidays = setOf(LocalDate.parse("2026-10-03"), LocalDate.parse("2026-10-05"), LocalDate.parse("2026-10-09"))
        holidays.forEach { date ->
            whenever(publicHolidayService.findPublicHoliday(date)).thenReturn(
                PublicHoliday(date = date, name = "holiday", calendarType = "solar"),
            )
        }

        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<String>>(
                """{ publicHolidays(start: "2026-10-01", end: "2026-10-12") }""",
                "data.publicHolidays",
            )

        assertEquals(listOf("2026-10-03", "2026-10-05", "2026-10-09"), result)
    }

    @Test
    fun `single date with no holiday returns empty list`() {
        val result = query("2026-10-01", "2026-10-01")

        assertTrue(result.errors.isEmpty())
        assertEquals(emptyList<String>(), result.getData<Map<String, Any>>()["publicHolidays"])
    }

    @Test
    fun `rejects start after end`() {
        val result = query("2026-04-01", "2026-03-01")

        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `rejects 62 day span and accepts 61 day span`() {
        val tooLong = query("2026-01-01", "2026-03-04")
        val accepted = query("2026-01-01", "2026-03-03")

        assertTrue(tooLong.errors.isNotEmpty())
        assertFalse(accepted.errors.isNotEmpty())
    }

    private fun query(
        start: String,
        end: String,
    ) = dgsQueryExecutor.execute(
        """{ publicHolidays(start: "$start", end: "$end") }""",
    )
}
