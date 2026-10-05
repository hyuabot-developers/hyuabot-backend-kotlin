package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.PublicHoliday
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource("classpath:application-test.properties")
class PublicHolidayRepositoryTest {
    @Autowired lateinit var repository: PublicHolidayRepository

    @Test
    fun `official holidays query hydrates source for non-empty results`() {
        val holiday =
            PublicHoliday(
                date = LocalDate.of(2099, 1, 2),
                name = "Test holiday",
                calendarType = "solar",
                source = "TEST_SOURCE",
            )
        repository.saveAndFlush(holiday)

        val result =
            repository.findOfficialHolidaysBetween(
                source = "TEST_SOURCE",
                calendarType = "solar",
                start = LocalDate.of(2099, 1, 1),
                end = LocalDate.of(2099, 1, 3),
            )

        assertEquals(1, result.size)
        assertEquals(holiday.date, result.single().date)
        assertEquals(holiday.name, result.single().name)
        assertEquals(holiday.source, result.single().source)
    }
}
