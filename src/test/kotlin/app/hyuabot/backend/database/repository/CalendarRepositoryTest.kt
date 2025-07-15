package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.CalendarCategory
import app.hyuabot.backend.database.entity.CalendarEvent
import app.hyuabot.backend.database.entity.CalendarVersion
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.ZonedDateTime

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource("classpath:application-test.properties")
class CalendarRepositoryTest {
    @Autowired lateinit var calendarEventRepository: CalendarEventRepository

    @Autowired lateinit var calendarCategoryRepository: CalendarCategoryRepository

    @Autowired lateinit var calendarVersionRepository: CalendarVersionRepository

    val currentTime = ZonedDateTime.now().toLocalDate()
    val category =
        CalendarCategory(
            name = "General",
            event = listOf(),
        )

    val calendarVersion =
        CalendarVersion(
            name = "1.0",
            createdAt = ZonedDateTime.now(),
        )

    @BeforeEach
    fun setUp() {
        calendarVersionRepository.save(calendarVersion)
        calendarCategoryRepository.save(category)
        calendarEventRepository.saveAll(
            listOf(
                CalendarEvent(
                    categoryID = category.id ?: 0,
                    title = "Event 1",
                    description = "Description for Event 1",
                    start = currentTime.minusDays(1),
                    end = currentTime.plusDays(1),
                    category = category,
                ),
            ),
        )
    }

    @AfterEach
    fun tearDown() {
        calendarEventRepository.deleteAll()
        calendarCategoryRepository.deleteAll()
        calendarVersionRepository.deleteAll()
    }

    @Test
    @DisplayName("카테고리 ID 별 학사 일정 목록 조회")
    fun testFindEventsByCategoryID() {
        val events = calendarEventRepository.findByCategoryID(category.id ?: 0)
        assert(events.isNotEmpty())
        assert(events[0].id != null)
        assert(events[0].categoryID == category.id)
        assert(events[0].category.id == category.id)
        assert(events[0].title == "Event 1")
        assert(events[0].description == "Description for Event 1")
        assert(events[0].start == currentTime.minusDays(1))
        assert(events[0].end == currentTime.plusDays(1))
    }

    @Test
    @DisplayName("학사 일정 목록 제목 키워드 검색")
    fun testFindEventsByTitleContaining() {
        val events = calendarEventRepository.findByTitleContaining("Event")
        assert(events.isNotEmpty())
        assert(events[0].title == "Event 1")
    }

    @Test
    @DisplayName("진행중인 학사 일정 목록 조회")
    fun testFindEventsByStartBeforeAndEndAfter() {
        val events = calendarEventRepository.findByStartBeforeAndEndAfter(currentTime, currentTime)
        assert(events.isNotEmpty())
    }

    @Test
    @DisplayName("학사력 카테고리 이름 키워드 검색")
    fun testFindCategoryByNameContaining() {
        val categories = calendarCategoryRepository.findByNameContaining("General")
        assert(categories.isNotEmpty())
        assert(categories[0].name == "General")
        assert(categories[0].id != null)
        assert(categories[0].event.isEmpty())
    }

    @Test
    @DisplayName("최신 학사력 버전 조회")
    fun testFindLatestCalendarVersion() {
        val latestVersion = calendarVersionRepository.findTopByOrderByCreatedAtDesc()
        assert(latestVersion != null)
        assert(latestVersion?.id != null)
        assert(latestVersion?.name == "1.0")
        assert(latestVersion?.createdAt != null)
    }
}
