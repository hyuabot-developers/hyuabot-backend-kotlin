package app.hyuabot.backend.holiday

import app.hyuabot.backend.database.entity.PublicHoliday
import app.hyuabot.backend.database.exception.LocalDateNotValidException
import app.hyuabot.backend.holiday.domain.PublicHolidayRequest
import app.hyuabot.backend.holiday.exception.DuplicatePublicHolidayException
import app.hyuabot.backend.holiday.exception.PublicHolidayNotFoundException
import app.hyuabot.backend.holiday.service.PublicHolidayService
import app.hyuabot.backend.security.WithCustomMockUser
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicHolidayControllerTest {
    @MockitoBean
    private lateinit var service: PublicHolidayService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DisplayName("공휴일 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetPublicHolidayList() {
        doReturn(
            listOf(
                PublicHoliday(seq = 1, date = LocalDate.parse("2025-01-01"), name = "신정", calendarType = "solar"),
                PublicHoliday(seq = 2, date = LocalDate.parse("2025-01-29"), name = "설날", calendarType = "lunar"),
            ),
        ).whenever(service).getPublicHolidayList()
        mockMvc
            .perform(get("/api/v1/holiday"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.result.length()").value(2))
            .andExpect(jsonPath("$.result[0].seq").value(1))
            .andExpect(jsonPath("$.result[0].date").value("2025-01-01"))
            .andExpect(jsonPath("$.result[0].name").value("신정"))
            .andExpect(jsonPath("$.result[0].calendarType").value("solar"))
            .andExpect(jsonPath("$.result[1].seq").value(2))
            .andExpect(jsonPath("$.result[1].date").value("2025-01-29"))
            .andExpect(jsonPath("$.result[1].name").value("설날"))
            .andExpect(jsonPath("$.result[1].calendarType").value("lunar"))
    }

    @Test
    @DisplayName("공휴일 항목 추가")
    @WithCustomMockUser(username = "test_user")
    fun testCreatePublicHoliday() {
        val newHoliday = PublicHoliday(seq = 1, date = LocalDate.parse("2025-03-01"), name = "삼일절", calendarType = "solar")
        doReturn(newHoliday).whenever(service).createPublicHoliday(
            PublicHolidayRequest(date = "2025-03-01", name = "삼일절", calendarType = "solar"),
        )
        mockMvc
            .perform(
                post("/api/v1/holiday")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            PublicHolidayRequest(date = "2025-03-01", name = "삼일절", calendarType = "solar"),
                        ),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.date").value("2025-03-01"))
            .andExpect(jsonPath("$.name").value("삼일절"))
            .andExpect(jsonPath("$.calendarType").value("solar"))
    }

    @Test
    @DisplayName("공휴일 항목 추가 - 날짜 형식 오류")
    @WithCustomMockUser(username = "test_user")
    fun testCreatePublicHolidayInvalidDateFormat() {
        doThrow(LocalDateNotValidException()).whenever(service).createPublicHoliday(
            PublicHolidayRequest(date = "2025/03/01", name = "삼일절", calendarType = "solar"),
        )
        mockMvc
            .perform(
                post("/api/v1/holiday")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            PublicHolidayRequest(date = "2025/03/01", name = "삼일절", calendarType = "solar"),
                        ),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("LOCAL_DATE_NOT_VALID"))
    }

    @Test
    @DisplayName("공휴일 항목 추가 - 잘못된 달력 유형")
    @WithCustomMockUser(username = "test_user")
    fun testCreatePublicHolidayInvalidCalendarType() {
        val request = PublicHolidayRequest(date = "2025-03-01", name = "삼일절", calendarType = "gregorian")
        doThrow(IllegalArgumentException()).whenever(service).createPublicHoliday(request)

        mockMvc
            .perform(
                post("/api/v1/holiday")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("INVALID_HOLIDAY_TYPE"))
    }

    @Test
    @DisplayName("공휴일 항목 추가 - 중복된 날짜")
    @WithCustomMockUser(username = "test_user")
    fun testCreatePublicHolidayDuplicateDate() {
        doThrow(DuplicatePublicHolidayException()).whenever(service).createPublicHoliday(
            PublicHolidayRequest(date = "2025-03-01", name = "삼일절", calendarType = "solar"),
        )
        mockMvc
            .perform(
                post("/api/v1/holiday")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            PublicHolidayRequest(date = "2025-03-01", name = "삼일절", calendarType = "solar"),
                        ),
                    ),
            ).andExpect(status().isConflict)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("DUPLICATE_PUBLIC_HOLIDAY"))
    }

    @Test
    @DisplayName("공휴일 항목 추가 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testCreatePublicHolidayOtherError() {
        doThrow(RuntimeException("Unexpected Error")).whenever(service).createPublicHoliday(
            PublicHolidayRequest(date = "2025-03-01", name = "삼일절", calendarType = "solar"),
        )
        mockMvc
            .perform(
                post("/api/v1/holiday")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            PublicHolidayRequest(date = "2025-03-01", name = "삼일절", calendarType = "solar"),
                        ),
                    ),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("공휴일 항목 단건 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetPublicHolidayById() {
        val holiday = PublicHoliday(seq = 1, date = LocalDate.of(2025, 1, 1), name = "신정", calendarType = "solar")
        whenever(service.getPublicHolidayById(1)) doReturn holiday
        mockMvc
            .perform(get("/api/v1/holiday/1"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.date").value("2025-01-01"))
            .andExpect(jsonPath("$.name").value("신정"))
            .andExpect(jsonPath("$.calendarType").value("solar"))
    }

    @Test
    @DisplayName("공휴일 항목 단건 조회 - 없는 ID")
    @WithCustomMockUser(username = "test_user")
    fun testGetPublicHolidayByIdNotFound() {
        whenever(service.getPublicHolidayById(999)) doThrow PublicHolidayNotFoundException()
        mockMvc
            .perform(get("/api/v1/holiday/999"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("PUBLIC_HOLIDAY_NOT_FOUND"))
    }

    @Test
    @DisplayName("공휴일 항목 단건 조회 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testGetPublicHolidayByIdOtherError() {
        whenever(service.getPublicHolidayById(1)) doThrow RuntimeException("Unexpected Error")
        mockMvc
            .perform(get("/api/v1/holiday/1"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("공휴일 항목 수정")
    @WithCustomMockUser(username = "test_user")
    fun testUpdatePublicHoliday() {
        val updated = PublicHoliday(seq = 1, date = LocalDate.parse("2025-03-02"), name = "삼일절 대체공휴일", calendarType = "solar")
        doReturn(updated).whenever(service).updatePublicHoliday(
            1,
            PublicHolidayRequest(date = "2025-03-02", name = "삼일절 대체공휴일", calendarType = "solar"),
        )
        mockMvc
            .perform(
                put("/api/v1/holiday/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            PublicHolidayRequest(date = "2025-03-02", name = "삼일절 대체공휴일", calendarType = "solar"),
                        ),
                    ),
            ).andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.date").value("2025-03-02"))
            .andExpect(jsonPath("$.name").value("삼일절 대체공휴일"))
            .andExpect(jsonPath("$.calendarType").value("solar"))
    }

    @Test
    @DisplayName("공휴일 항목 수정 - 날짜 형식 오류")
    @WithCustomMockUser(username = "test_user")
    fun testUpdatePublicHolidayInvalidDateFormat() {
        doThrow(LocalDateNotValidException()).whenever(service).updatePublicHoliday(
            1,
            PublicHolidayRequest(date = "2025/03/02", name = "테스트", calendarType = "solar"),
        )
        mockMvc
            .perform(
                put("/api/v1/holiday/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            PublicHolidayRequest(date = "2025/03/02", name = "테스트", calendarType = "solar"),
                        ),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("LOCAL_DATE_NOT_VALID"))
    }

    @Test
    @DisplayName("공휴일 항목 수정 - 잘못된 달력 유형")
    @WithCustomMockUser(username = "test_user")
    fun testUpdatePublicHolidayInvalidCalendarType() {
        val request = PublicHolidayRequest(date = "2025-03-01", name = "삼일절", calendarType = "gregorian")
        doThrow(IllegalArgumentException()).whenever(service).updatePublicHoliday(1, request)

        mockMvc
            .perform(
                put("/api/v1/holiday/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("INVALID_HOLIDAY_TYPE"))
    }

    @Test
    @DisplayName("공휴일 항목 수정 - 없는 ID")
    @WithCustomMockUser(username = "test_user")
    fun testUpdatePublicHolidayNotFound() {
        doThrow(PublicHolidayNotFoundException()).whenever(service).updatePublicHoliday(
            999,
            PublicHolidayRequest(date = "2025-03-01", name = "테스트", calendarType = "solar"),
        )
        mockMvc
            .perform(
                put("/api/v1/holiday/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            PublicHolidayRequest(date = "2025-03-01", name = "테스트", calendarType = "solar"),
                        ),
                    ),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("PUBLIC_HOLIDAY_NOT_FOUND"))
    }

    @Test
    @DisplayName("공휴일 항목 수정 - 중복된 날짜")
    @WithCustomMockUser(username = "test_user")
    fun testUpdatePublicHolidayDuplicateDate() {
        doThrow(DuplicatePublicHolidayException()).whenever(service).updatePublicHoliday(
            1,
            PublicHolidayRequest(date = "2025-03-01", name = "테스트", calendarType = "solar"),
        )
        mockMvc
            .perform(
                put("/api/v1/holiday/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            PublicHolidayRequest(date = "2025-03-01", name = "테스트", calendarType = "solar"),
                        ),
                    ),
            ).andExpect(status().isConflict)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("DUPLICATE_PUBLIC_HOLIDAY"))
    }

    @Test
    @DisplayName("공휴일 항목 수정 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testUpdatePublicHolidayOtherError() {
        doThrow(RuntimeException("Unexpected Error")).whenever(service).updatePublicHoliday(
            1,
            PublicHolidayRequest(date = "2025-03-01", name = "테스트", calendarType = "solar"),
        )
        mockMvc
            .perform(
                put("/api/v1/holiday/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            PublicHolidayRequest(date = "2025-03-01", name = "테스트", calendarType = "solar"),
                        ),
                    ),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("공휴일 항목 삭제")
    @WithCustomMockUser(username = "test_user")
    fun testDeletePublicHoliday() {
        mockMvc
            .perform(delete("/api/v1/holiday/1").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("공휴일 항목 삭제 - 없는 ID")
    @WithCustomMockUser(username = "test_user")
    fun testDeletePublicHolidayNotFound() {
        doThrow(PublicHolidayNotFoundException()).whenever(service).deletePublicHoliday(999)
        mockMvc
            .perform(delete("/api/v1/holiday/999").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("PUBLIC_HOLIDAY_NOT_FOUND"))
    }

    @Test
    @DisplayName("공휴일 항목 삭제 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testDeletePublicHolidayOtherError() {
        doThrow(RuntimeException("Unexpected Error")).whenever(service).deletePublicHoliday(1)
        mockMvc
            .perform(delete("/api/v1/holiday/1").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }
}
