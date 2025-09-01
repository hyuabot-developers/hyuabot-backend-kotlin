package app.hyuabot.backend.calendar

import app.hyuabot.backend.calendar.domain.CalendarCategoryRequest
import app.hyuabot.backend.calendar.domain.CalendarEventRequest
import app.hyuabot.backend.calendar.exception.CalendarCategoryNotFoundException
import app.hyuabot.backend.calendar.exception.CalendarEventNotFoundException
import app.hyuabot.backend.calendar.exception.DuplicateCategoryException
import app.hyuabot.backend.database.entity.CalendarCategory
import app.hyuabot.backend.database.entity.CalendarEvent
import app.hyuabot.backend.database.entity.CalendarVersion
import app.hyuabot.backend.database.exception.LocalDateTimeNotValidException
import app.hyuabot.backend.security.WithCustomMockUser
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.ZonedDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CalendarControllerTest {
    @MockitoBean
    private lateinit var calendarService: CalendarService

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DisplayName("학사일정 버전 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetCalendarVersion() {
        doReturn(
            CalendarVersion(
                id = 1,
                name = "1.0",
                createdAt = ZonedDateTime.now(),
            ),
        ).whenever(calendarService).getCalendarVersion()
        mockMvc
            .perform(get("/api/v1/calendar/version"))
            .andExpect(status().isOk)
    }

    @Test
    @DisplayName("학사일정 카테고리 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetCalendarCategories() {
        doReturn(
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
        ).whenever(calendarService).getCalendarCategories()
        mockMvc
            .perform(get("/api/v1/calendar/category"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.length()").value(2))
            .andExpect(jsonPath("$.result[0].name").value("General"))
            .andExpect(jsonPath("$.result[1].name").value("Exam"))
    }

    @Test
    @DisplayName("학사일정 카테고리 생성")
    @WithCustomMockUser(username = "test_user")
    fun testCreateCalendarCategory() {
        val request = CalendarCategoryRequest(name = "General")
        doReturn(
            CalendarCategory(
                id = 1,
                name = "General",
                event = emptyList(),
            ),
        ).whenever(calendarService).createCalendarCategory(request)
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/calendar/category")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.name").value("General"))
    }

    @Test
    @DisplayName("학사일정 카테고리 생성 - 중복된 이름")
    @WithCustomMockUser(username = "test_user")
    fun testCreateCalendarCategoryWithDuplicateName() {
        val request = CalendarCategoryRequest(name = "General")
        doThrow(DuplicateCategoryException()).whenever(calendarService).createCalendarCategory(request)
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/calendar/category")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("DUPLICATE_CATEGORY_NAME"))
    }

    @Test
    @DisplayName("학사일정 카테고리 생성 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testCreateCalendarCategoryWithException() {
        val request = CalendarCategoryRequest(name = "General")
        doThrow(RuntimeException("Unexpected Error")).whenever(calendarService).createCalendarCategory(request)
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/calendar/category")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("학사일정 카테고리 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetCalendarCategoryById() {
        doReturn(
            CalendarCategory(
                id = 1,
                name = "General",
                event = emptyList(),
            ),
        ).whenever(calendarService).getCalendarCategoryById(1)
        mockMvc
            .perform(get("/api/v1/calendar/category/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.name").value("General"))
    }

    @Test
    @DisplayName("학사일정 카테고리 조회 - 존재하지 않는 카테고리")
    @WithCustomMockUser(username = "test_user")
    fun testGetCalendarCategoryByIdNotFound() {
        doThrow(CalendarCategoryNotFoundException()).whenever(calendarService).getCalendarCategoryById(999)
        mockMvc
            .perform(get("/api/v1/calendar/category/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("CATEGORY_NOT_FOUND"))
    }

    @Test
    @DisplayName("학사일정 카테고리 조회 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testGetCalendarCategoryByIdWithException() {
        doThrow(RuntimeException("Unexpected Error")).whenever(calendarService).getCalendarCategoryById(1)
        mockMvc
            .perform(get("/api/v1/calendar/category/1"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("학사일정 카테고리 삭제")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteCalendarCategoryById() {
        mockMvc
            .perform(delete("/api/v1/calendar/category/1"))
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("학사일정 카테고리 삭제 - 존재하지 않는 카테고리")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteCalendarCategoryByIdNotFound() {
        doThrow(CalendarCategoryNotFoundException()).whenever(calendarService).deleteCalendarCategoryById(999)
        mockMvc
            .perform(delete("/api/v1/calendar/category/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("CATEGORY_NOT_FOUND"))
    }

    @Test
    @DisplayName("학사일정 카테고리 삭제 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteCalendarCategoryByIdWithException() {
        doThrow(RuntimeException("Unexpected Error")).whenever(calendarService).deleteCalendarCategoryById(1)
        mockMvc
            .perform(delete("/api/v1/calendar/category/1"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("학사일정 카테고리별 일정 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetEventsByCategoryId() {
        whenever(calendarService.getEventByCategoryId(1)).doReturn(
            listOf(
                CalendarEvent(
                    id = 1,
                    title = "Event 1",
                    description = "Description for Event 1",
                    start = LocalDate.parse("2020-01-01"),
                    end = LocalDate.parse("2020-01-02"),
                    categoryID = 1,
                    category = null,
                ),
            ),
        )
        mockMvc
            .perform(get("/api/v1/calendar/category/1/events"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.length()").value(1))
    }

    @Test
    @DisplayName("학사일정 카테고리별 일정 조회 - 존재하지 않는 카테고리")
    @WithCustomMockUser(username = "test_user")
    fun testGetEventsByCategoryIdNotFound() {
        doThrow(CalendarCategoryNotFoundException()).whenever(calendarService).getEventByCategoryId(999)
        mockMvc
            .perform(get("/api/v1/calendar/category/999/events"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("CATEGORY_NOT_FOUND"))
    }

    @Test
    @DisplayName("학사일정 일정 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetAllEvents() {
        whenever(calendarService.getAllEvents()).doReturn(
            listOf(
                CalendarEvent(
                    id = 1,
                    title = "Event 1",
                    description = "Description for Event 1",
                    start = LocalDate.parse("2020-01-01"),
                    end = LocalDate.parse("2020-01-02"),
                    categoryID = 1,
                    category = null,
                ),
                CalendarEvent(
                    id = 2,
                    title = "Event 2",
                    description = "Description for Event 2",
                    start = LocalDate.parse("2020-02-01"),
                    end = LocalDate.parse("2020-02-02"),
                    categoryID = 1,
                    category = null,
                ),
            ),
        )
        mockMvc
            .perform(get("/api/v1/calendar/events"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.length()").value(2))
    }

    @Test
    @DisplayName("학사일정 일정 생성")
    @WithCustomMockUser(username = "test_user")
    fun testCreateEvent() {
        val request =
            CalendarEventRequest(
                title = "Event 1",
                description = "Description for Event 1",
                categoryID = 1,
                start = "2020-01-01",
                end = "2020-01-02",
            )
        whenever(calendarService.createEvent(request)).doReturn(
            CalendarEvent(
                id = 1,
                title = "Event 1",
                description = "Description for Event 1",
                start = LocalDate.parse("2020-01-01"),
                end = LocalDate.parse("2020-01-02"),
                categoryID = 1,
                category = null,
            ),
        )
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/calendar/events")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.title").value("Event 1"))
            .andExpect(jsonPath("$.description").value("Description for Event 1"))
            .andExpect(jsonPath("$.categoryID").value(1))
            .andExpect(jsonPath("$.start").value("2020-01-01"))
            .andExpect(jsonPath("$.end").value("2020-01-02"))
    }

    @Test
    @DisplayName("학사일정 일정 생성 - 존재하지 않는 카테고리")
    @WithCustomMockUser(username = "test_user")
    fun testCreateEventWithCategoryNotFound() {
        val request =
            CalendarEventRequest(
                title = "Event 1",
                description = "Description for Event 1",
                categoryID = 999,
                start = "2020-01-01",
                end = "2020-01-02",
            )
        doThrow(CalendarCategoryNotFoundException()).whenever(calendarService).createEvent(request)
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/calendar/events")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("CATEGORY_NOT_FOUND"))
    }

    @Test
    @DisplayName("학사일정 일정 생성 - 잘못된 날짜 형식")
    @WithCustomMockUser(username = "test_user")
    fun testCreateEventWithInvalidDate() {
        val request =
            CalendarEventRequest(
                title = "Event 1",
                description = "Description for Event 1",
                categoryID = 1,
                start = "invalid-date",
                end = "2020-01-02",
            )
        doThrow(LocalDateTimeNotValidException()).whenever(calendarService).createEvent(request)
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/calendar/events")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("LOCAL_DATE_TIME_NOT_VALID"))
    }

    @Test
    @DisplayName("학사일정 일정 생성 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testCreateEventWithException() {
        val request =
            CalendarEventRequest(
                title = "Event 1",
                description = "Description for Event 1",
                categoryID = 1,
                start = "2020-01-01",
                end = "2020-01-02",
            )
        doThrow(RuntimeException("Unexpected Error")).whenever(calendarService).createEvent(request)
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/calendar/events")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("학사일정 일정 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetEventById() {
        whenever(calendarService.getEventById(1)).doReturn(
            CalendarEvent(
                id = 1,
                title = "Event 1",
                description = "Description for Event 1",
                start = LocalDate.parse("2020-01-01"),
                end = LocalDate.parse("2020-01-02"),
                categoryID = 1,
                category = null,
            ),
        )
        mockMvc
            .perform(get("/api/v1/calendar/events/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.title").value("Event 1"))
            .andExpect(jsonPath("$.description").value("Description for Event 1"))
            .andExpect(jsonPath("$.categoryID").value(1))
            .andExpect(jsonPath("$.start").value("2020-01-01"))
            .andExpect(jsonPath("$.end").value("2020-01-02"))
    }

    @Test
    @DisplayName("학사일정 일정 조회 - 존재하지 않는 일정")
    @WithCustomMockUser(username = "test_user")
    fun testGetEventByIdNotFound() {
        doThrow(CalendarEventNotFoundException()).whenever(calendarService).getEventById(999)
        mockMvc
            .perform(get("/api/v1/calendar/events/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("EVENT_NOT_FOUND"))
    }

    @Test
    @DisplayName("학사일정 일정 조회 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testGetEventByIdWithException() {
        doThrow(RuntimeException("Unexpected Error")).whenever(calendarService).getEventById(1)
        mockMvc
            .perform(get("/api/v1/calendar/events/1"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("학사일정 일정 수정")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateEvent() {
        val request =
            CalendarEventRequest(
                title = "Updated Event",
                description = "Updated Description",
                categoryID = 1,
                start = "2020-01-03",
                end = "2020-01-04",
            )
        whenever(calendarService.updateEvent(1, request)).doReturn(
            CalendarEvent(
                id = 1,
                title = "Updated Event",
                description = "Updated Description",
                start = LocalDate.parse("2020-01-03"),
                end = LocalDate.parse("2020-01-04"),
                categoryID = 1,
                category = null,
            ),
        )
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .put("/api/v1/calendar/events/1")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.title").value("Updated Event"))
            .andExpect(jsonPath("$.description").value("Updated Description"))
            .andExpect(jsonPath("$.categoryID").value(1))
            .andExpect(jsonPath("$.start").value("2020-01-03"))
            .andExpect(jsonPath("$.end").value("2020-01-04"))
    }

    @Test
    @DisplayName("학사일정 일정 수정 - 존재하지 않는 일정")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateEventNotFound() {
        val request =
            CalendarEventRequest(
                title = "Updated Event",
                description = "Updated Description",
                categoryID = 1,
                start = "2020-01-03",
                end = "2020-01-04",
            )
        doThrow(CalendarEventNotFoundException()).whenever(calendarService).updateEvent(999, request)
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .put("/api/v1/calendar/events/999")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("EVENT_NOT_FOUND"))
    }

    @Test
    @DisplayName("학사일정 일정 수정 - 존재하지 않는 카테고리")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateEventWithCategoryNotFound() {
        val request =
            CalendarEventRequest(
                title = "Updated Event",
                description = "Updated Description",
                categoryID = 999,
                start = "2020-01-03",
                end = "2020-01-04",
            )
        doThrow(CalendarCategoryNotFoundException()).whenever(calendarService).updateEvent(1, request)
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .put("/api/v1/calendar/events/1")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("CATEGORY_NOT_FOUND"))
    }

    @Test
    @DisplayName("학사일정 일정 수정 - 잘못된 날짜 형식")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateEventWithInvalidDate() {
        val request =
            CalendarEventRequest(
                title = "Updated Event",
                description = "Updated Description",
                categoryID = 1,
                start = "invalid-date",
                end = "2020-01-04",
            )
        doThrow(LocalDateTimeNotValidException()).whenever(calendarService).updateEvent(1, request)
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .put("/api/v1/calendar/events/1")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("LOCAL_DATE_TIME_NOT_VALID"))
    }

    @Test
    @DisplayName("학사일정 일정 수정 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateEventWithException() {
        val request =
            CalendarEventRequest(
                title = "Updated Event",
                description = "Updated Description",
                categoryID = 1,
                start = "2020-01-03",
                end = "2020-01-04",
            )
        doThrow(RuntimeException("Unexpected Error")).whenever(calendarService).updateEvent(1, request)
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .put("/api/v1/calendar/events/1")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("학사일정 일정 삭제")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteEventById() {
        mockMvc
            .perform(delete("/api/v1/calendar/events/1"))
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("학사일정 일정 삭제 - 존재하지 않는 일정")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteEventByIdNotFound() {
        doThrow(CalendarEventNotFoundException()).whenever(calendarService).deleteEventById(999)
        mockMvc
            .perform(delete("/api/v1/calendar/events/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("EVENT_NOT_FOUND"))
    }

    @Test
    @DisplayName("학사일정 일정 삭제 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteEventByIdWithException() {
        doThrow(RuntimeException("Unexpected Error")).whenever(calendarService).deleteEventById(1)
        mockMvc
            .perform(delete("/api/v1/calendar/events/1"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }
}
