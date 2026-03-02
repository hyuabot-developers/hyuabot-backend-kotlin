package app.hyuabot.backend.calendar

import app.hyuabot.backend.calendar.controller.CalendarDataFetcher
import app.hyuabot.backend.database.entity.CalendarCategory
import app.hyuabot.backend.database.entity.CalendarEvent
import app.hyuabot.backend.database.entity.CalendarVersion
import app.hyuabot.backend.utility.ScalarRegistration
import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.test.EnableDgsTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.assertEquals

@EnableDgsTest
@SpringJUnitConfig
@Import(CalendarDataFetcher::class, ScalarRegistration::class)
class CalendarDataFetcherTest {
    @Autowired lateinit var dgsQueryExecutor: DgsQueryExecutor

    @MockitoBean lateinit var calendarService: CalendarService

    private val now = ZonedDateTime.now()

    private fun createEvent(
        id: Int = 1,
        title: String = "Test Event",
        description: String = "Test Description",
        start: LocalDate = LocalDate.parse("2025-03-01"),
        end: LocalDate = LocalDate.parse("2025-03-31"),
        categoryID: Int = 1,
    ) = CalendarEvent(
        id = id,
        title = title,
        description = description,
        start = start,
        end = end,
        categoryID = categoryID,
        category = null,
    )

    private fun createCategory(
        id: Int = 1,
        name: String = "학사",
        events: MutableList<CalendarEvent> = mutableListOf(),
    ) = CalendarCategory(id = id, name = name, event = events)

    @Test
    @DisplayName("학사 일정 카테고리와 이벤트를 올바르게 반환하는지 테스트")
    fun testFetchCalendarEvents() {
        whenever(
            calendarService.getCalendarVersion(),
        ).thenReturn(
            CalendarVersion(name = "1.0", createdAt = now),
        )
        whenever(calendarService.fetchCalendarEvents(anyOrNull(), any(), any())).thenReturn(
            listOf(
                createCategory(
                    id = 1,
                    name = "학사",
                    events =
                        mutableListOf(
                            createEvent(
                                id = 1,
                                title = "개강",
                                description = "2025-03-01에 개강합니다.",
                                start = LocalDate.parse("2025-03-01"),
                                end = LocalDate.parse("2025-03-01"),
                            ),
                            createEvent(
                                id = 2,
                                title = "중간고사",
                                description = "2025-03-15에 중간고사가 있습니다.",
                                start = LocalDate.parse("2025-03-15"),
                                end = LocalDate.parse("2025-03-15"),
                            ),
                        ),
                ),
            ),
        )
        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<Map<String, Any>>(
                """
                {
                    calendar {
                        version
                        categories {
                            seq
                            name
                            events {
                                seq
                                title
                                description
                                start
                                end
                            }
                        }
                    }
                }
                """.trimIndent(),
                "data.calendar",
            )
        assertEquals("1.0", result["version"])
        val categories = result["categories"] as List<*>
        assertEquals(1, categories.size)
        val category = categories[0] as Map<*, *>
        assertEquals(1, category["seq"])
        assertEquals("학사", category["name"])
        val events = category["events"] as List<*>
        assertEquals(2, events.size)
        val event1 = events[0] as Map<*, *>
        assertEquals(1, event1["seq"])
        assertEquals("개강", event1["title"])
        assertEquals("2025-03-01에 개강합니다.", event1["description"])
        assertEquals("2025-03-01", event1["start"])
        assertEquals("2025-03-01", event1["end"])
        val event2 = events[1] as Map<*, *>
        assertEquals(2, event2["seq"])
        assertEquals("중간고사", event2["title"])
        assertEquals("2025-03-15에 중간고사가 있습니다.", event2["description"])
        assertEquals("2025-03-15", event2["start"])
        assertEquals("2025-03-15", event2["end"])
    }

    @Test
    @DisplayName("학사 일정을 카테고리로 필터링하여 반환하는지 테스트")
    fun testFetchCalendarEventsWithCategoryFilter() {
        whenever(
            calendarService.fetchCalendarEvents(
                eq("학사"),
                anyOrNull(),
                anyOrNull(),
            ),
        ).thenReturn(
            listOf(
                createCategory(
                    id = 1,
                    name = "학사",
                    events =
                        mutableListOf(
                            createEvent(
                                id = 1,
                                title = "개강",
                                description = "2025-03-01에 개강합니다.",
                                start = LocalDate.parse("2025-03-01"),
                                end = LocalDate.parse("2025-03-01"),
                            ),
                        ),
                ),
            ),
        )
        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<Map<String, Any>>(
                """
                {
                    calendar(input: { category: "학사" }) {
                        categories {
                            seq
                            name
                            events {
                                seq
                                title
                                description
                                start
                                end
                            }
                        }
                    }
                }
                """.trimIndent(),
                "data.calendar",
            )
        val categories = result["categories"] as List<*>
        assertEquals(1, categories.size)
        val category = categories[0] as Map<*, *>
        assertEquals(1, category["seq"])
        assertEquals("학사", category["name"])
    }

    @Test
    @DisplayName("학사 일정을 날짜 범위로 필터링하여 반환하는지 테스트")
    fun testFetchCalendarEventsWithDateRangeFilter() {
        whenever(
            calendarService.fetchCalendarEvents(
                anyOrNull(),
                eq(LocalDate.parse("2025-03-01")),
                eq(LocalDate.parse("2025-03-31")),
            ),
        ).thenReturn(
            listOf(
                createCategory(
                    id = 1,
                    name = "학사",
                    events =
                        mutableListOf(
                            createEvent(
                                id = 1,
                                title = "개강",
                                description = "2025-03-01에 개강합니다.",
                                start = LocalDate.parse("2025-03-01"),
                                end = LocalDate.parse("2025-03-01"),
                            ),
                            createEvent(
                                id = 2,
                                title = "중간고사",
                                description = "2025-03-15에 중간고사가 있습니다.",
                                start = LocalDate.parse("2025-03-15"),
                                end = LocalDate.parse("2025-03-15"),
                            ),
                        ),
                ),
            ),
        )
        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<Map<String, Any>>(
                """
                {
                    calendar(input: { start: "2025-03-01", end: "2025-03-31" }) {
                        categories {
                            seq
                            name
                            events {
                                seq
                                title
                                description
                                start
                                end
                            }
                        }
                    }
                }
                """.trimIndent(),
                "data.calendar",
            )
        val categories = result["categories"] as List<*>
        assertEquals(1, categories.size)
        val category = categories[0] as Map<*, *>
        assertEquals(1, category["seq"])
        assertEquals("학사", category["name"])
        val events = category["events"] as List<*>
        assertEquals(2, events.size)
        val event1 = events[0] as Map<*, *>
        assertEquals(1, event1["seq"])
        assertEquals("개강", event1["title"])
        assertEquals("2025-03-01에 개강합니다.", event1["description"])
        assertEquals("2025-03-01", event1["start"])
        assertEquals("2025-03-01", event1["end"])
        val event2 = events[1] as Map<*, *>
        assertEquals(2, event2["seq"])
        assertEquals("중간고사", event2["title"])
        assertEquals("2025-03-15에 중간고사가 있습니다.", event2["description"])
        assertEquals("2025-03-15", event2["start"])
        assertEquals("2025-03-15", event2["end"])
    }
}
