package app.hyuabot.backend.calendar

import app.hyuabot.backend.calendar.domain.CalendarCategoryRequest
import app.hyuabot.backend.calendar.domain.CalendarEventRequest
import app.hyuabot.backend.calendar.exception.CalendarCategoryNotFoundException
import app.hyuabot.backend.calendar.exception.CalendarEventNotFoundException
import app.hyuabot.backend.calendar.exception.CalendarVersionNotFoundException
import app.hyuabot.backend.calendar.exception.DuplicateCategoryException
import app.hyuabot.backend.database.entity.CalendarCategory
import app.hyuabot.backend.database.entity.CalendarEvent
import app.hyuabot.backend.database.entity.CalendarVersion
import app.hyuabot.backend.database.exception.LocalDateTimeNotValidException
import app.hyuabot.backend.database.repository.CalendarCategoryRepository
import app.hyuabot.backend.database.repository.CalendarEventRepository
import app.hyuabot.backend.database.repository.CalendarVersionRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Optional
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class CalendarServiceTest {
    @Mock
    lateinit var categoryRepository: CalendarCategoryRepository

    @Mock
    lateinit var eventRepository: CalendarEventRepository

    @Mock
    lateinit var versionRepository: CalendarVersionRepository

    @InjectMocks
    lateinit var service: CalendarService

    @Test
    @DisplayName("학사일정 버전 조회 테스트")
    fun testGetCalendarVersion() {
        whenever(versionRepository.findTopByOrderByCreatedAtDesc()).thenReturn(
            CalendarVersion(
                id = 1,
                name = "1.0",
                createdAt = ZonedDateTime.now(),
            ),
        )
        val version = service.getCalendarVersion()
        assertEquals("1.0", version.name)
    }

    @Test
    @DisplayName("학사일정 버전 조회 실패 테스트 - 버전 없음")
    fun testGetCalendarVersionNotFound() {
        whenever(versionRepository.findTopByOrderByCreatedAtDesc()).thenReturn(null)
        assertThrows<CalendarVersionNotFoundException> { service.getCalendarVersion() }
    }

    @Test
    @DisplayName("학사일정 카테고리 목록")
    fun testGetCalendarCategories() {
        whenever(categoryRepository.findAll()).thenReturn(
            listOf(
                CalendarCategory(
                    id = 1,
                    name = "General",
                    event = emptyList(),
                ),
                CalendarCategory(
                    id = 2,
                    name = "Exam",
                    event = emptyList(),
                ),
            ),
        )
        val categories = service.getCalendarCategories()
        assertEquals(2, categories.size)
        assertEquals("General", categories[0].name)
        assertEquals("Exam", categories[1].name)
    }

    @Test
    @DisplayName("학사일정 카테고리 생성")
    fun testCreateCalendarCategory() {
        val payload = CalendarCategoryRequest(name = "Holiday")
        val newCategory =
            CalendarCategory(
                id = null,
                name = payload.name,
                event = emptyList(),
            )
        whenever(categoryRepository.findByName(payload.name)).thenReturn(null)
        whenever(
            categoryRepository.save(newCategory),
        ).thenReturn(
            CalendarCategory(
                id = 1,
                name = payload.name,
                event = emptyList(),
            ),
        )
        val category = service.createCalendarCategory(payload)
        assertEquals(1, category.id)
        assertEquals("Holiday", category.name)
    }

    @Test
    @DisplayName("학사일정 카테고리 생성 실패 - 중복된 이름")
    fun testCreateCalendarCategoryDuplicateName() {
        val payload = CalendarCategoryRequest(name = "General")
        whenever(categoryRepository.findByName(payload.name)).thenReturn(
            CalendarCategory(
                id = 1,
                name = payload.name,
                event = emptyList(),
            ),
        )
        assertThrows<DuplicateCategoryException> { service.createCalendarCategory(payload) }
    }

    @Test
    @DisplayName("학사일정 카테고리 항목 조회 - ID로 조회")
    fun testGetCalendarCategoryById() {
        val categoryId = 1
        whenever(categoryRepository.findById(categoryId)).thenReturn(
            Optional.of(
                CalendarCategory(
                    id = categoryId,
                    name = "General",
                    event = emptyList(),
                ),
            ),
        )
        val category = service.getCalendarCategoryById(categoryId)
        assertEquals(categoryId, category.id)
        assertEquals("General", category.name)
    }

    @Test
    @DisplayName("학사일정 카테고리 항목 조회 실패 - ID로 조회")
    fun testGetCalendarCategoryByIdNotFound() {
        val categoryId = 1
        whenever(categoryRepository.findById(categoryId)).thenReturn(Optional.empty())
        assertThrows<CalendarCategoryNotFoundException> { service.getCalendarCategoryById(categoryId) }
    }

    @Test
    @DisplayName("학사일정 카테고리 삭제 - ID로 삭제")
    fun testDeleteCalendarCategoryById() {
        val categoryId = 1
        val category =
            CalendarCategory(
                id = categoryId,
                name = "General",
                event = emptyList(),
            )
        whenever(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category))
        service.deleteCalendarCategoryById(categoryId)
    }

    @Test
    @DisplayName("학사일정 카테고리 삭제 실패 - ID로 삭제")
    fun testDeleteCalendarCategoryByIdNotFound() {
        val categoryId = 1
        whenever(categoryRepository.findById(categoryId)).thenReturn(Optional.empty())
        assertThrows<CalendarCategoryNotFoundException> { service.deleteCalendarCategoryById(categoryId) }
    }

    @Test
    @DisplayName("학사일정 전체 목록 조회")
    fun testGetAllEvents() {
        whenever(eventRepository.findAll()).thenReturn(
            listOf(
                // 이벤트 ID 순으로 정렬됨
                CalendarEvent(
                    id = 2,
                    categoryID = 1,
                    title = "Event 2",
                    description = "Description for Event 2",
                    start =
                        LocalDate
                            .now()
                            .minusDays(2),
                    end =
                        LocalDate
                            .now()
                            .plusDays(2),
                    category = null,
                ),
                CalendarEvent(
                    id = 1,
                    categoryID = 1,
                    title = "Event 1",
                    description = "Description for Event 1",
                    start =
                        LocalDate
                            .now()
                            .minusDays(1),
                    end =
                        LocalDate
                            .now()
                            .plusDays(1),
                    category = null,
                ),
            ),
        )
        val events = service.getAllEvents()
        assertEquals(2, events.size)
        assertEquals(1, events[0].id)
        assertEquals("Event 1", events[0].title)
        assertEquals(2, events[1].id)
        assertEquals("Event 2", events[1].title)
    }

    @Test
    @DisplayName("학사일정 카테고리 ID 별 학사 일정 목록 조회")
    fun testGetEventByCategoryId() {
        val categoryId = 1
        val category =
            CalendarCategory(
                id = categoryId,
                name = "General",
                event =
                    listOf(
                        CalendarEvent(
                            id = 2,
                            categoryID = categoryId,
                            title = "Event 2",
                            description = "Description for Event 2",
                            start =
                                LocalDate
                                    .now()
                                    .minusDays(2),
                            end =
                                LocalDate
                                    .now()
                                    .plusDays(2),
                            category = null,
                        ),
                        CalendarEvent(
                            id = 1,
                            categoryID = categoryId,
                            title = "Event 1",
                            description = "Description for Event 1",
                            start =
                                LocalDate
                                    .now()
                                    .minusDays(1),
                            end =
                                LocalDate
                                    .now()
                                    .plusDays(1),
                            category = null,
                        ),
                    ),
            )
        whenever(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category))
        val events = service.getEventByCategoryId(categoryId)
        assertEquals(2, events.size)
        assertEquals(1, events[0].id)
        assertEquals("Event 1", events[0].title)
        assertEquals(2, events[1].id)
        assertEquals("Event 2", events[1].title)
    }

    @Test
    @DisplayName("학사일정 카테고리 ID 별 학사 일정 목록 조회 실패 - 카테고리 없음")
    fun testGetEventByCategoryIdNotFound() {
        val categoryId = 1
        whenever(categoryRepository.findById(categoryId)).thenReturn(Optional.empty())
        assertThrows<CalendarCategoryNotFoundException> { service.getEventByCategoryId(categoryId) }
    }

    @Test
    @DisplayName("학사일정 항목 생성")
    fun testCreateEvent() {
        val payload =
            CalendarEventRequest(
                title = "Midterm Exam",
                description = "Midterm exam period",
                categoryID = 1,
                start = LocalDate.now().plusDays(1).toString(),
                end = LocalDate.now().plusDays(5).toString(),
            )
        whenever(categoryRepository.findById(payload.categoryID)).thenReturn(
            Optional.of(
                CalendarCategory(
                    id = payload.categoryID,
                    name = "Exam",
                    event = emptyList(),
                ),
            ),
        )
        val newEvent =
            CalendarEvent(
                id = null,
                title = payload.title,
                description = payload.description,
                categoryID = payload.categoryID,
                start = LocalDate.parse(payload.start),
                end = LocalDate.parse(payload.end),
                category = null,
            )
        whenever(eventRepository.save(newEvent)).thenReturn(
            CalendarEvent(
                id = 1,
                title = payload.title,
                description = payload.description,
                categoryID = payload.categoryID,
                start = LocalDate.parse(payload.start),
                end = LocalDate.parse(payload.end),
                category = null,
            ),
        )
        val event = service.createEvent(payload)
        assertEquals(1, event.id)
        assertEquals("Midterm Exam", event.title)
        assertEquals("Midterm exam period", event.description)
        assertEquals(1, event.categoryID)
        assertEquals(LocalDate.parse(payload.start), event.start)
        assertEquals(LocalDate.parse(payload.end), event.end)
    }

    @Test
    @DisplayName("학사일정 항목 생성 실패 - 잘못된 날짜 범위")
    fun testCreateEventInvalidDateRange() {
        val payload =
            CalendarEventRequest(
                title = "Midterm Exam",
                description = "Midterm exam period",
                categoryID = 1,
                start = LocalDate.now().plusDays(5).toString(),
                end = LocalDate.now().plusDays(1).toString(),
            )
        assertThrows<LocalDateTimeNotValidException> { service.createEvent(payload) }
    }

    @Test
    @DisplayName("학사일정 항목 생성 실패 - 잘못된 날짜 형식")
    fun testCreateEventInvalidDateFormat() {
        val payload =
            CalendarEventRequest(
                title = "Midterm Exam",
                description = "Midterm exam period",
                categoryID = 1,
                start = "2023-13-01",
                end = "2023-12-32",
            )
        assertThrows<LocalDateTimeNotValidException> { service.createEvent(payload) }
    }

    @Test
    @DisplayName("학사일정 항목 생성 실패 - 카테고리 없음")
    fun testCreateEventCategoryNotFound() {
        val payload =
            CalendarEventRequest(
                title = "Midterm Exam",
                description = "Midterm exam period",
                categoryID = 1,
                start = LocalDate.now().plusDays(1).toString(),
                end = LocalDate.now().plusDays(5).toString(),
            )
        whenever(categoryRepository.findById(payload.categoryID)).thenReturn(Optional.empty())
        assertThrows<CalendarCategoryNotFoundException> { service.createEvent(payload) }
    }

    @Test
    @DisplayName("학사일정 항목 조회 - ID로 조회")
    fun testGetEventById() {
        val eventId = 1
        whenever(eventRepository.findById(eventId)).thenReturn(
            Optional.of(
                CalendarEvent(
                    id = eventId,
                    categoryID = 1,
                    title = "Event 1",
                    description = "Description for Event 1",
                    start = LocalDate.now().minusDays(1),
                    end = LocalDate.now().plusDays(1),
                    category = null,
                ),
            ),
        )
        val event = service.getEventById(eventId)
        assertEquals(eventId, event.id)
        assertEquals("Event 1", event.title)
    }

    @Test
    @DisplayName("학사일정 항목 조회 실패 - ID로 조회")
    fun testGetEventByIdNotFound() {
        val eventId = 1
        whenever(eventRepository.findById(eventId)).thenReturn(Optional.empty())
        assertThrows<CalendarEventNotFoundException> { service.getEventById(eventId) }
    }

    @Test
    @DisplayName("학사일정 항목 수정")
    fun testUpdateEvent() {
        val eventId = 1
        val payload =
            CalendarEventRequest(
                title = "Final Exam",
                description = "Final exam period",
                categoryID = 2,
                start = LocalDate.now().plusDays(10).toString(),
                end = LocalDate.now().plusDays(15).toString(),
            )
        whenever(categoryRepository.findById(payload.categoryID)).thenReturn(
            Optional.of(
                CalendarCategory(
                    id = payload.categoryID,
                    name = "Exam",
                    event = emptyList(),
                ),
            ),
        )
        val existingEvent =
            CalendarEvent(
                id = eventId,
                title = "Midterm Exam",
                description = "Midterm exam period",
                categoryID = 1,
                start = LocalDate.now().plusDays(1),
                end = LocalDate.now().plusDays(5),
                category = null,
            )
        whenever(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent))
        val updatedEvent =
            CalendarEvent(
                id = eventId,
                title = payload.title,
                description = payload.description,
                categoryID = payload.categoryID,
                start = LocalDate.parse(payload.start),
                end = LocalDate.parse(payload.end),
                category = null,
            )
        whenever(eventRepository.save(updatedEvent)).thenReturn(updatedEvent)
        val event = service.updateEvent(eventId, payload)
        assertEquals(eventId, event.id)
        assertEquals("Final Exam", event.title)
        assertEquals("Final exam period", event.description)
        assertEquals(2, event.categoryID)
        assertEquals(LocalDate.parse(payload.start), event.start)
        assertEquals(LocalDate.parse(payload.end), event.end)
    }

    @Test
    @DisplayName("학사일정 항목 수정 실패 - 잘못된 날짜 범위")
    fun testUpdateEventInvalidDateRange() {
        val eventId = 1
        val payload =
            CalendarEventRequest(
                title = "Final Exam",
                description = "Final exam period",
                categoryID = 2,
                start = LocalDate.now().plusDays(15).toString(),
                end = LocalDate.now().plusDays(10).toString(),
            )
        assertThrows<LocalDateTimeNotValidException> { service.updateEvent(eventId, payload) }
    }

    @Test
    @DisplayName("학사일정 항목 수정 실패 - 잘못된 날짜 형식")
    fun testUpdateEventInvalidDateFormat() {
        val eventId = 1
        val payload =
            CalendarEventRequest(
                title = "Final Exam",
                description = "Final exam period",
                categoryID = 2,
                start = "2023-13-01",
                end = "2023-12-32",
            )
        assertThrows<LocalDateTimeNotValidException> { service.updateEvent(eventId, payload) }
    }

    @Test
    @DisplayName("학사일정 항목 수정 실패 - 카테고리 없음")
    fun testUpdateEventCategoryNotFound() {
        val eventId = 1
        val payload =
            CalendarEventRequest(
                title = "Final Exam",
                description = "Final exam period",
                categoryID = 2,
                start = LocalDate.now().plusDays(10).toString(),
                end = LocalDate.now().plusDays(15).toString(),
            )
        whenever(categoryRepository.findById(payload.categoryID)).thenReturn(Optional.empty())
        assertThrows<CalendarCategoryNotFoundException> { service.updateEvent(eventId, payload) }
    }

    @Test
    @DisplayName("학사일정 항목 수정 실패 - ID로 조회")
    fun testUpdateEventByIdNotFound() {
        val eventId = 1
        val payload =
            CalendarEventRequest(
                title = "Final Exam",
                description = "Final exam period",
                categoryID = 2,
                start = LocalDate.now().plusDays(10).toString(),
                end = LocalDate.now().plusDays(15).toString(),
            )
        whenever(categoryRepository.findById(payload.categoryID)).thenReturn(
            Optional.of(
                CalendarCategory(
                    id = payload.categoryID,
                    name = "Exam",
                    event = emptyList(),
                ),
            ),
        )
        whenever(eventRepository.findById(eventId)).thenReturn(Optional.empty())
        assertThrows<CalendarEventNotFoundException> { service.updateEvent(eventId, payload) }
    }

    @Test
    @DisplayName("학사일정 항목 삭제 - ID로 삭제")
    fun testDeleteEventById() {
        val eventId = 1
        val existingEvent =
            CalendarEvent(
                id = eventId,
                title = "Midterm Exam",
                description = "Midterm exam period",
                categoryID = 1,
                start = LocalDate.now().plusDays(1),
                end = LocalDate.now().plusDays(5),
                category = null,
            )
        whenever(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent))
        service.deleteEventById(eventId)
    }

    @Test
    @DisplayName("학사일정 항목 삭제 실패 - ID로 삭제")
    fun testDeleteEventByIdNotFound() {
        val eventId = 1
        whenever(eventRepository.findById(eventId)).thenReturn(Optional.empty())
        assertThrows<CalendarEventNotFoundException> { service.deleteEventById(eventId) }
    }

    @Test
    @DisplayName("학사일정 버전 업데이트 호출 테스트")
    fun testUpdateCalendarVersion() {
        val now = ZonedDateTime.now()
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val newVersion =
            CalendarVersion(
                id = 1,
                name = now.format(dateTimeFormatter),
                createdAt = now,
            )
        whenever(versionRepository.findAll()).thenReturn(listOf(newVersion))
        whenever(versionRepository.save(newVersion)).thenReturn(newVersion)
        service.updateCalendarVersion()
    }
}
