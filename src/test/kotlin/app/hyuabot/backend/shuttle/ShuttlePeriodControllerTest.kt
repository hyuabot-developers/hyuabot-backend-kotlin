package app.hyuabot.backend.shuttle

import app.hyuabot.backend.database.entity.ShuttlePeriod
import app.hyuabot.backend.database.exception.LocalDateTimeNotValidException
import app.hyuabot.backend.security.WithCustomMockUser
import app.hyuabot.backend.shuttle.domain.ShuttlePeriodRequest
import app.hyuabot.backend.shuttle.exception.ShuttlePeriodNotFoundException
import app.hyuabot.backend.shuttle.service.ShuttlePeriodService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doNothing
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
import java.time.ZonedDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShuttlePeriodControllerTest {
    @MockitoBean
    private lateinit var shuttlePeriodService: ShuttlePeriodService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DisplayName("셔틀버스 운행 기간 조회")
    @WithCustomMockUser(username = "test_user")
    fun getShuttlePeriod() {
        doReturn(
            listOf(
                ShuttlePeriod(
                    seq = 1,
                    start = ZonedDateTime.parse("2025-09-01T00:00:00.000+09:00"),
                    end = ZonedDateTime.parse("2025-12-23T23:59:59.999+09:00"),
                    type = "semester",
                    periodType = null,
                ),
                ShuttlePeriod(
                    seq = 2,
                    start = ZonedDateTime.parse("2025-12-24T00:00:00.000+09:00"),
                    end = ZonedDateTime.parse("2026-01-15T23:59:59.999+09:00"),
                    type = "vacation_session",
                    periodType = null,
                ),
                ShuttlePeriod(
                    seq = 3,
                    start = ZonedDateTime.parse("2026-01-16T00:00:00.000+09:00"),
                    end = ZonedDateTime.parse("2026-02-28T23:59:59.999+09:00"),
                    type = "vacation",
                    periodType = null,
                ),
            ),
        ).whenever(shuttlePeriodService).getShuttlePeriodList()
        mockMvc
            .perform(get("/api/v1/shuttle/period"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.result").isArray)
            .andExpect(jsonPath("$.result.length()").value(3))
            .andExpect(jsonPath("$.result[0].seq").value(1))
            .andExpect(jsonPath("$.result[0].start").value("2025-09-01 00:00:00"))
            .andExpect(jsonPath("$.result[0].end").value("2025-12-23 23:59:59"))
            .andExpect(jsonPath("$.result[0].type").value("semester"))
            .andExpect(jsonPath("$.result[1].seq").value(2))
            .andExpect(jsonPath("$.result[1].start").value("2025-12-24 00:00:00"))
            .andExpect(jsonPath("$.result[1].end").value("2026-01-15 23:59:59"))
            .andExpect(jsonPath("$.result[1].type").value("vacation_session"))
            .andExpect(jsonPath("$.result[2].seq").value(3))
            .andExpect(jsonPath("$.result[2].start").value("2026-01-16 00:00:00"))
            .andExpect(jsonPath("$.result[2].end").value("2026-02-28 23:59:59"))
            .andExpect(jsonPath("$.result[2].type").value("vacation"))
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 추가")
    @WithCustomMockUser(username = "test_user")
    fun addShuttlePeriod() {
        val newPeriod =
            ShuttlePeriod(
                seq = 1,
                start = ZonedDateTime.parse("2026-03-01T00:00:00.000+09:00"),
                end = ZonedDateTime.parse("2026-06-30T23:59:59.999+09:00"),
                type = "semester",
                periodType = null,
            )

        doReturn(newPeriod).whenever(shuttlePeriodService).createShuttlePeriod(
            ShuttlePeriodRequest(
                start = "2026-03-01 00:00:00",
                end = "2026-06-30 23:59:59",
                type = "semester",
            ),
        )

        mockMvc
            .perform(
                post("/api/v1/shuttle/period")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttlePeriodRequest(
                                start = "2026-03-01 00:00:00",
                                end = "2026-06-30 23:59:59",
                                type = "semester",
                            ),
                        ),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.start").value("2026-03-01 00:00:00"))
            .andExpect(jsonPath("$.end").value("2026-06-30 23:59:59"))
            .andExpect(jsonPath("$.type").value("semester"))
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 추가 (잘못된 시간 형식)")
    @WithCustomMockUser(username = "test_user")
    fun addShuttlePeriodInvalidTimeFormat() {
        doThrow(LocalDateTimeNotValidException::class)
            .whenever(
                shuttlePeriodService,
            ).createShuttlePeriod(
                ShuttlePeriodRequest(
                    start = "2026-03-01 00:00:00",
                    end = "2026-06-30 23:59:60",
                    type = "semester",
                ),
            )

        mockMvc
            .perform(
                post("/api/v1/shuttle/period")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttlePeriodRequest(
                                start = "2026-03-01 00:00:00",
                                end = "2026-06-30 23:59:60",
                                type = "semester",
                            ),
                        ),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INVALID_DATE_TIME_FORMAT"))
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 추가 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun addShuttlePeriodOtherError() {
        doThrow(IllegalArgumentException("Start time must be before end time"))
            .whenever(shuttlePeriodService)
            .createShuttlePeriod(
                ShuttlePeriodRequest(
                    start = "2026-06-30 23:59:59",
                    end = "2026-03-01 00:00:00",
                    type = "semester",
                ),
            )

        mockMvc
            .perform(
                post("/api/v1/shuttle/period")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttlePeriodRequest(
                                start = "2026-06-30 23:59:59",
                                end = "2026-03-01 00:00:00",
                                type = "semester",
                            ),
                        ),
                    ),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 조회")
    @WithCustomMockUser(username = "test_user")
    fun getShuttlePeriodById() {
        val period =
            ShuttlePeriod(
                seq = 1,
                start = ZonedDateTime.parse("2025-09-01T00:00:00.000+09:00"),
                end = ZonedDateTime.parse("2025-12-23T23:59:59.999+09:00"),
                type = "semester",
                periodType = null,
            )

        doReturn(period).whenever(shuttlePeriodService).getShuttlePeriod(1)

        mockMvc
            .perform(get("/api/v1/shuttle/period/1"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.start").value("2025-09-01 00:00:00"))
            .andExpect(jsonPath("$.end").value("2025-12-23 23:59:59"))
            .andExpect(jsonPath("$.type").value("semester"))
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 조회 (존재하지 않는 ID)")
    @WithCustomMockUser(username = "test_user")
    fun getShuttlePeriodByIdNotFound() {
        doThrow(ShuttlePeriodNotFoundException::class)
            .whenever(shuttlePeriodService)
            .getShuttlePeriod(999)

        mockMvc
            .perform(get("/api/v1/shuttle/period/999"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("SHUTTLE_PERIOD_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 조회 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun getShuttlePeriodByIdOtherError() {
        doThrow(IllegalArgumentException("Invalid period ID"))
            .whenever(shuttlePeriodService)
            .getShuttlePeriod(-1)

        mockMvc
            .perform(get("/api/v1/shuttle/period/-1"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 수정")
    @WithCustomMockUser(username = "test_user")
    fun updateShuttlePeriod() {
        val updatedPeriod =
            ShuttlePeriod(
                seq = 1,
                start = ZonedDateTime.parse("2025-09-01T00:00:00.000+09:00"),
                end = ZonedDateTime.parse("2025-12-23T23:59:59.999+09:00"),
                type = "semester",
                periodType = null,
            )

        doReturn(updatedPeriod).whenever(shuttlePeriodService).updateShuttlePeriod(
            1,
            ShuttlePeriodRequest(
                start = "2025-09-01 00:00:00",
                end = "2025-12-23 23:59:59",
                type = "semester",
            ),
        )

        mockMvc
            .perform(
                put("/api/v1/shuttle/period/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttlePeriodRequest(
                                start = "2025-09-01 00:00:00",
                                end = "2025-12-23 23:59:59",
                                type = "semester",
                            ),
                        ),
                    ),
            ).andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.start").value("2025-09-01 00:00:00"))
            .andExpect(jsonPath("$.end").value("2025-12-23 23:59:59"))
            .andExpect(jsonPath("$.type").value("semester"))
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 수정 (잘못된 시간 형식)")
    @WithCustomMockUser(username = "test_user")
    fun updateShuttlePeriodInvalidTimeFormat() {
        doThrow(LocalDateTimeNotValidException::class)
            .whenever(shuttlePeriodService)
            .updateShuttlePeriod(
                1,
                ShuttlePeriodRequest(
                    start = "2025-09-01 00:00:00",
                    end = "2025-12-23 23:59:60",
                    type = "semester",
                ),
            )

        mockMvc
            .perform(
                put("/api/v1/shuttle/period/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttlePeriodRequest(
                                start = "2025-09-01 00:00:00",
                                end = "2025-12-23 23:59:60",
                                type = "semester",
                            ),
                        ),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INVALID_DATE_TIME_FORMAT"))
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 수정 (존재하지 않는 ID)")
    @WithCustomMockUser(username = "test_user")
    fun updateShuttlePeriodNotFound() {
        doThrow(ShuttlePeriodNotFoundException::class)
            .whenever(shuttlePeriodService)
            .updateShuttlePeriod(
                999,
                ShuttlePeriodRequest(
                    start = "2025-09-01 00:00:00",
                    end = "2025-12-23 23:59:59",
                    type = "semester",
                ),
            )

        mockMvc
            .perform(
                put("/api/v1/shuttle/period/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttlePeriodRequest(
                                start = "2025-09-01 00:00:00",
                                end = "2025-12-23 23:59:59",
                                type = "semester",
                            ),
                        ),
                    ),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("SHUTTLE_PERIOD_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 수정 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun updateShuttlePeriodOtherError() {
        doThrow(IllegalArgumentException("Start time must be before end time"))
            .whenever(shuttlePeriodService)
            .updateShuttlePeriod(
                1,
                ShuttlePeriodRequest(
                    start = "2025-12-23 23:59:59",
                    end = "2025-09-01 00:00:00",
                    type = "semester",
                ),
            )

        mockMvc
            .perform(
                put("/api/v1/shuttle/period/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttlePeriodRequest(
                                start = "2025-12-23 23:59:59",
                                end = "2025-09-01 00:00:00",
                                type = "semester",
                            ),
                        ),
                    ),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 삭제")
    @WithCustomMockUser(username = "test_user")
    fun deleteShuttlePeriod() {
        doNothing().whenever(shuttlePeriodService).deleteShuttlePeriod(1)

        mockMvc
            .perform(delete("/api/v1/shuttle/period/1"))
            .andExpect(status().isNoContent)
            .andExpect(content().contentType("application/json"))
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 삭제 (존재하지 않는 ID)")
    @WithCustomMockUser(username = "test_user")
    fun deleteShuttlePeriodNotFound() {
        doThrow(ShuttlePeriodNotFoundException::class)
            .whenever(shuttlePeriodService)
            .deleteShuttlePeriod(999)

        mockMvc
            .perform(delete("/api/v1/shuttle/period/999"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("SHUTTLE_PERIOD_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 삭제 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun deleteShuttlePeriodOtherError() {
        doThrow(IllegalArgumentException("Invalid period ID"))
            .whenever(shuttlePeriodService)
            .deleteShuttlePeriod(-1)

        mockMvc
            .perform(delete("/api/v1/shuttle/period/-1"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }
}
