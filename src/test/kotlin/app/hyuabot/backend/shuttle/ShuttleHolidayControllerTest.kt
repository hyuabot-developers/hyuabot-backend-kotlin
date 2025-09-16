package app.hyuabot.backend.shuttle

import app.hyuabot.backend.database.entity.ShuttleHoliday
import app.hyuabot.backend.database.exception.LocalDateNotValidException
import app.hyuabot.backend.security.WithCustomMockUser
import app.hyuabot.backend.shuttle.domain.ShuttleHolidayRequest
import app.hyuabot.backend.shuttle.exception.DuplicateShuttleHolidayException
import app.hyuabot.backend.shuttle.exception.ShuttleHolidayNotFoundException
import app.hyuabot.backend.shuttle.service.ShuttleHolidayService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
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
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShuttleHolidayControllerTest {
    @MockitoBean
    private lateinit var service: ShuttleHolidayService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DisplayName("셔틀 공휴일 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetShuttleHolidayList() {
        doReturn(
            listOf(
                ShuttleHoliday(
                    seq = 1,
                    date = LocalDate.parse("2020-09-09"),
                    calendarType = "solar",
                    type = "New Year's Day",
                ),
                ShuttleHoliday(
                    seq = 2,
                    date = LocalDate.parse("2020-10-10"),
                    calendarType = "lunar",
                    type = "Lunar New Year",
                ),
            ),
        ).whenever(service).getShuttleHolidayList()
        mockMvc
            .perform(get("/api/v1/shuttle/holiday"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.result.length()").value(2))
            .andExpect(jsonPath("$.result[0].seq").value(1))
            .andExpect(jsonPath("$.result[0].date").value("2020-09-09"))
            .andExpect(jsonPath("$.result[0].calendarType").value("solar"))
            .andExpect(jsonPath("$.result[0].type").value("New Year's Day"))
            .andExpect(jsonPath("$.result[1].seq").value(2))
            .andExpect(jsonPath("$.result[1].date").value("2020-10-10"))
            .andExpect(jsonPath("$.result[1].calendarType").value("lunar"))
            .andExpect(jsonPath("$.result[1].type").value("Lunar New Year"))
    }

    @Test
    @DisplayName("셔틀 공휴일 추가")
    @WithCustomMockUser(username = "test_user")
    fun testAddShuttleHoliday() {
        val newHoliday =
            ShuttleHoliday(
                seq = 3,
                date = LocalDate.parse("2020-12-25"),
                calendarType = "solar",
                type = "weekends",
            )
        doReturn(newHoliday).whenever(service).createShuttleHoliday(
            ShuttleHolidayRequest(
                date = "2020-12-25",
                calendarType = "solar",
                type = "weekends",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/shuttle/holiday")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttleHolidayRequest(
                                date = "2020-12-25",
                                calendarType = "solar",
                                type = "weekends",
                            ),
                        ),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.seq").value(3))
            .andExpect(jsonPath("$.date").value("2020-12-25"))
            .andExpect(jsonPath("$.calendarType").value("solar"))
            .andExpect(jsonPath("$.type").value("weekends"))
    }

    @Test
    @DisplayName("셔틀 공휴일 추가 - 날짜 형식 오류")
    @WithCustomMockUser(username = "test_user")
    fun testAddShuttleHolidayInvalidDateFormat() {
        doThrow(LocalDateNotValidException()).whenever(service).createShuttleHoliday(
            ShuttleHolidayRequest(
                date = "2020/12/25",
                calendarType = "solar",
                type = "weekends",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/shuttle/holiday")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttleHolidayRequest(
                                date = "2020/12/25",
                                calendarType = "solar",
                                type = "weekends",
                            ),
                        ),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("LOCAL_DATE_NOT_VALID"))
    }

    @Test
    @DisplayName("셔틀 공휴일 추가 - 중복된 날짜")
    @WithCustomMockUser(username = "test_user")
    fun testAddShuttleHolidayDuplicateDate() {
        doThrow(DuplicateShuttleHolidayException()).whenever(service).createShuttleHoliday(
            ShuttleHolidayRequest(
                date = "2020-12-25",
                calendarType = "solar",
                type = "weekends",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/shuttle/holiday")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttleHolidayRequest(
                                date = "2020-12-25",
                                calendarType = "solar",
                                type = "weekends",
                            ),
                        ),
                    ),
            ).andExpect(status().isConflict)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("DUPLICATE_SHUTTLE_HOLIDAY"))
    }

    @Test
    @DisplayName("셔틀 공휴일 추가 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testAddShuttleHolidayOtherError() {
        doThrow(RuntimeException("Unexpected Error")).whenever(service).createShuttleHoliday(
            ShuttleHolidayRequest(
                date = "2020-12-25",
                calendarType = "solar",
                type = "weekends",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/shuttle/holiday")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttleHolidayRequest(
                                date = "2020-12-25",
                                calendarType = "solar",
                                type = "weekends",
                            ),
                        ),
                    ),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("셔틀 공휴일 항목 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetShuttleHolidayById() {
        val holiday =
            ShuttleHoliday(
                seq = 1,
                date = LocalDate.of(2024, 1, 1),
                calendarType = "solar",
                type = "weekends",
            )
        whenever(service.getShuttleHolidayById(1)) doReturn holiday
        mockMvc
            .perform(get("/api/v1/shuttle/holiday/1"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.date").value("2024-01-01"))
            .andExpect(jsonPath("$.calendarType").value("solar"))
            .andExpect(jsonPath("$.type").value("weekends"))
    }

    @Test
    @DisplayName("셔틀 공휴일 항목 조회 - 없는 ID")
    @WithCustomMockUser(username = "test_user")
    fun testGetShuttleHolidayByIdNotFound() {
        whenever(service.getShuttleHolidayById(999)) doThrow ShuttleHolidayNotFoundException()
        mockMvc
            .perform(get("/api/v1/shuttle/holiday/999"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SHUTTLE_HOLIDAY_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀 공휴일 항목 조회 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testGetShuttleHolidayByIdOtherError() {
        whenever(service.getShuttleHolidayById(1)) doThrow RuntimeException("Unexpected Error")
        mockMvc
            .perform(get("/api/v1/shuttle/holiday/1"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("셔틀 공휴일 항목 수정")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateShuttleHoliday() {
        val updatedHoliday =
            ShuttleHoliday(
                seq = 1,
                date = LocalDate.parse("2020-12-31"),
                calendarType = "solar",
                type = "New Year's Eve",
            )
        doReturn(updatedHoliday).whenever(service).updateShuttleHoliday(
            1,
            ShuttleHolidayRequest(
                date = "2020-12-31",
                calendarType = "solar",
                type = "New Year's Eve",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/shuttle/holiday/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttleHolidayRequest(
                                date = "2020-12-31",
                                calendarType = "solar",
                                type = "New Year's Eve",
                            ),
                        ),
                    ),
            ).andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.date").value("2020-12-31"))
            .andExpect(jsonPath("$.calendarType").value("solar"))
            .andExpect(jsonPath("$.type").value("New Year's Eve"))
    }

    @Test
    @DisplayName("셔틀 공휴일 항목 수정 - 없는 ID")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateShuttleHolidayNotFound() {
        doThrow(ShuttleHolidayNotFoundException()).whenever(service).updateShuttleHoliday(
            999,
            ShuttleHolidayRequest(
                date = "2020-12-31",
                calendarType = "solar",
                type = "New Year's Eve",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/shuttle/holiday/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttleHolidayRequest(
                                date = "2020-12-31",
                                calendarType = "solar",
                                type = "New Year's Eve",
                            ),
                        ),
                    ),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SHUTTLE_HOLIDAY_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀 공휴일 항목 수정 - 날짜 형식 오류")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateShuttleHolidayInvalidDateFormat() {
        doThrow(LocalDateNotValidException()).whenever(service).updateShuttleHoliday(
            1,
            ShuttleHolidayRequest(
                date = "2020/12/31",
                calendarType = "solar",
                type = "New Year's Eve",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/shuttle/holiday/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttleHolidayRequest(
                                date = "2020/12/31",
                                calendarType = "solar",
                                type = "New Year's Eve",
                            ),
                        ),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("LOCAL_DATE_NOT_VALID"))
    }

    @Test
    @DisplayName("셔틀 공휴일 항목 수정 - 중복된 날짜")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateShuttleHolidayDuplicateDate() {
        doThrow(DuplicateShuttleHolidayException()).whenever(service).updateShuttleHoliday(
            1,
            ShuttleHolidayRequest(
                date = "2020-12-25",
                calendarType = "solar",
                type = "Christmas",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/shuttle/holiday/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttleHolidayRequest(
                                date = "2020-12-25",
                                calendarType = "solar",
                                type = "Christmas",
                            ),
                        ),
                    ),
            ).andExpect(status().isConflict)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("DUPLICATE_SHUTTLE_HOLIDAY"))
    }

    @Test
    @DisplayName("셔틀 공휴일 항목 수정 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateShuttleHolidayOtherError() {
        doThrow(RuntimeException("Unexpected Error")).whenever(service).updateShuttleHoliday(
            1,
            ShuttleHolidayRequest(
                date = "2020-12-31",
                calendarType = "solar",
                type = "New Year's Eve",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/shuttle/holiday/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttleHolidayRequest(
                                date = "2020-12-31",
                                calendarType = "solar",
                                type = "New Year's Eve",
                            ),
                        ),
                    ),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("셔틀 공휴일 항목 삭제")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteShuttleHoliday() {
        mockMvc
            .perform(
                delete("/api/v1/shuttle/holiday/1")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("셔틀 공휴일 항목 삭제 - 없는 ID")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteShuttleHolidayNotFound() {
        doThrow(ShuttleHolidayNotFoundException()).whenever(service).deleteShuttleHoliday(999)
        mockMvc
            .perform(
                delete("/api/v1/shuttle/holiday/999")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SHUTTLE_HOLIDAY_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀 공휴일 항목 삭제 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteShuttleHolidayOtherError() {
        doThrow(RuntimeException("Unexpected Error")).whenever(service).deleteShuttleHoliday(1)
        mockMvc
            .perform(
                delete("/api/v1/shuttle/holiday/1")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }
}
