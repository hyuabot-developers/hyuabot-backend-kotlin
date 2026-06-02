package app.hyuabot.backend.commuteShuttle

import app.hyuabot.backend.commuteShuttle.domain.CreateShuttleRouteRequest
import app.hyuabot.backend.commuteShuttle.domain.CreateShuttleStopRequest
import app.hyuabot.backend.commuteShuttle.domain.ShuttleTimetableRequest
import app.hyuabot.backend.commuteShuttle.domain.UpdateShuttleRouteRequest
import app.hyuabot.backend.commuteShuttle.domain.UpdateShuttleStopRequest
import app.hyuabot.backend.commuteShuttle.exception.DuplicateShuttleRouteException
import app.hyuabot.backend.commuteShuttle.exception.DuplicateShuttleStopException
import app.hyuabot.backend.commuteShuttle.exception.ShuttleRouteNotFoundException
import app.hyuabot.backend.commuteShuttle.exception.ShuttleStopNotFoundException
import app.hyuabot.backend.commuteShuttle.exception.ShuttleTimetableNotFoundException
import app.hyuabot.backend.database.entity.CommuteShuttleRoute
import app.hyuabot.backend.database.entity.CommuteShuttleStop
import app.hyuabot.backend.database.entity.CommuteShuttleTimetable
import app.hyuabot.backend.database.exception.LocalTimeNotValidException
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.time.LocalTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommuteShuttleControllerTest {
    @MockitoBean
    private lateinit var service: CommuteShuttleService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DisplayName("통학버스 노선 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetShuttleRoutes() {
        doReturn(
            listOf(
                CommuteShuttleRoute(
                    name = "TEST_ROUTE_1",
                    descriptionKorean = "테스트 노선 1",
                    descriptionEnglish = "Test Route 1",
                    timetable = mutableListOf(),
                ),
                CommuteShuttleRoute(
                    name = "TEST_ROUTE_2",
                    descriptionKorean = "테스트 노선 2",
                    descriptionEnglish = "Test Route 2",
                    timetable = mutableListOf(),
                ),
            ),
        ).whenever(service).getAllRoutes()
        mockMvc
            .perform(get("/api/v1/commute-shuttle/route"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result").isArray)
            .andExpect(jsonPath("$.result.length()").value(2))
            .andExpect(jsonPath("$.result[0].name").value("TEST_ROUTE_1"))
            .andExpect(jsonPath("$.result[0].descriptionKorean").value("테스트 노선 1"))
            .andExpect(jsonPath("$.result[0].descriptionEnglish").value("Test Route 1"))
            .andExpect(jsonPath("$.result[1].name").value("TEST_ROUTE_2"))
            .andExpect(jsonPath("$.result[1].descriptionKorean").value("테스트 노선 2"))
            .andExpect(jsonPath("$.result[1].descriptionEnglish").value("Test Route 2"))
    }

    @Test
    @DisplayName("통학버스 노선 생성")
    @WithCustomMockUser(username = "test_user")
    fun testCreateShuttleRoute() {
        doReturn(
            CommuteShuttleRoute(
                name = "TEST_ROUTE_1",
                descriptionKorean = "테스트 노선 1",
                descriptionEnglish = "Test Route 1",
                timetable = mutableListOf(),
            ),
        ).whenever(service).createRoute(
            CreateShuttleRouteRequest(
                name = "TEST_ROUTE_1",
                descriptionKorean = "테스트 노선 1",
                descriptionEnglish = "Test Route 1",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/commute-shuttle/route")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            CreateShuttleRouteRequest(
                                name = "TEST_ROUTE_1",
                                descriptionKorean = "테스트 노선 1",
                                descriptionEnglish = "Test Route 1",
                            ),
                        ),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("TEST_ROUTE_1"))
            .andExpect(jsonPath("$.descriptionKorean").value("테스트 노선 1"))
            .andExpect(jsonPath("$.descriptionEnglish").value("Test Route 1"))
    }

    @Test
    @DisplayName("통학버스 노선 생성 - 중복된 이름")
    @WithCustomMockUser(username = "test_user")
    fun testCreateShuttleRouteDuplicateName() {
        doThrow(DuplicateShuttleRouteException()).whenever(service).createRoute(
            CreateShuttleRouteRequest(
                name = "TEST_ROUTE_1",
                descriptionKorean = "테스트 노선 1",
                descriptionEnglish = "Test Route 1",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/commute-shuttle/route")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            CreateShuttleRouteRequest(
                                name = "TEST_ROUTE_1",
                                descriptionKorean = "테스트 노선 1",
                                descriptionEnglish = "Test Route 1",
                            ),
                        ),
                    ),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("DUPLICATE_SHUTTLE_ROUTE"))
    }

    @Test
    @DisplayName("통학버스 노선 생성 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testCreateShuttleRouteOtherException() {
        doThrow(RuntimeException()).whenever(service).createRoute(
            CreateShuttleRouteRequest(
                name = "TEST_ROUTE_1",
                descriptionKorean = "테스트 노선 1",
                descriptionEnglish = "Test Route 1",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/commute-shuttle/route")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            CreateShuttleRouteRequest(
                                name = "TEST_ROUTE_1",
                                descriptionKorean = "테스트 노선 1",
                                descriptionEnglish = "Test Route 1",
                            ),
                        ),
                    ),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("통학버스 노선 항목 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetShuttleRoute() {
        doReturn(
            CommuteShuttleRoute(
                name = "TEST_ROUTE_1",
                descriptionKorean = "테스트 노선 1",
                descriptionEnglish = "Test Route 1",
                timetable = mutableListOf(),
            ),
        ).whenever(service).getRouteByName("TEST_ROUTE_1")
        mockMvc
            .perform(get("/api/v1/commute-shuttle/route/TEST_ROUTE_1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("TEST_ROUTE_1"))
            .andExpect(jsonPath("$.descriptionKorean").value("테스트 노선 1"))
            .andExpect(jsonPath("$.descriptionEnglish").value("Test Route 1"))
    }

    @Test
    @DisplayName("통학버스 노선 항목 조회 - 존재하지 않는 노선")
    @WithCustomMockUser(username = "test_user")
    fun testGetShuttleRouteNotFound() {
        doThrow(ShuttleRouteNotFoundException()).whenever(service).getRouteByName("TEST_ROUTE_1")
        mockMvc
            .perform(get("/api/v1/commute-shuttle/route/TEST_ROUTE_1"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("통학버스 노선 항목 조회 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testGetShuttleRouteOtherException() {
        doThrow(RuntimeException()).whenever(service).getRouteByName("TEST_ROUTE_1")
        mockMvc
            .perform(get("/api/v1/commute-shuttle/route/TEST_ROUTE_1"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("통학버스 노선 수정")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateShuttleRoute() {
        doReturn(
            CommuteShuttleRoute(
                name = "TEST_ROUTE_1",
                descriptionKorean = "테스트 노선 1 - 수정됨",
                descriptionEnglish = "Test Route 1 - Updated",
                timetable = mutableListOf(),
            ),
        ).whenever(service).updateRoute(
            "TEST_ROUTE_1",
            UpdateShuttleRouteRequest(
                descriptionKorean = "테스트 노선 1 - 수정됨",
                descriptionEnglish = "Test Route 1 - Updated",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/commute-shuttle/route/TEST_ROUTE_1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            UpdateShuttleRouteRequest(
                                descriptionKorean = "테스트 노선 1 - 수정됨",
                                descriptionEnglish = "Test Route 1 - Updated",
                            ),
                        ),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("TEST_ROUTE_1"))
            .andExpect(jsonPath("$.descriptionKorean").value("테스트 노선 1 - 수정됨"))
            .andExpect(jsonPath("$.descriptionEnglish").value("Test Route 1 - Updated"))
    }

    @Test
    @DisplayName("통학버스 노선 수정 - 존재하지 않는 노선")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateShuttleRouteNotFound() {
        doThrow(ShuttleRouteNotFoundException()).whenever(service).updateRoute(
            "TEST_ROUTE_1",
            UpdateShuttleRouteRequest(
                descriptionKorean = "테스트 노선 1 - 수정됨",
                descriptionEnglish = "Test Route 1 - Updated",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/commute-shuttle/route/TEST_ROUTE_1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            UpdateShuttleRouteRequest(
                                descriptionKorean = "테스트 노선 1 - 수정됨",
                                descriptionEnglish = "Test Route 1 - Updated",
                            ),
                        ),
                    ),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("통학버스 노선 수정 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateShuttleRouteOtherException() {
        doThrow(RuntimeException()).whenever(service).updateRoute(
            "TEST_ROUTE_1",
            UpdateShuttleRouteRequest(
                descriptionKorean = "테스트 노선 1 - 수정됨",
                descriptionEnglish = "Test Route 1 - Updated",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/commute-shuttle/route/TEST_ROUTE_1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            UpdateShuttleRouteRequest(
                                descriptionKorean = "테스트 노선 1 - 수정됨",
                                descriptionEnglish = "Test Route 1 - Updated",
                            ),
                        ),
                    ),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("통학버스 노선 삭제")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteShuttleRoute() {
        mockMvc
            .perform(
                delete("/api/v1/commute-shuttle/route/TEST_ROUTE_1"),
            ).andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("통학버스 노선 삭제 - 존재하지 않는 노선")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteShuttleRouteNotFound() {
        doThrow(ShuttleRouteNotFoundException()).whenever(service).deleteRoute("TEST_ROUTE_1")
        mockMvc
            .perform(
                delete("/api/v1/commute-shuttle/route/TEST_ROUTE_1"),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("통학버스 노선 삭제 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteShuttleRouteOtherException() {
        doThrow(RuntimeException()).whenever(service).deleteRoute("TEST_ROUTE_1")
        mockMvc
            .perform(
                delete("/api/v1/commute-shuttle/route/TEST_ROUTE_1"),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetShuttleTimetableByRoute() {
        doReturn(
            listOf(
                CommuteShuttleTimetable(
                    seq = 1,
                    routeName = "TEST_ROUTE_1",
                    stopName = "TEST_STOP_1",
                    order = 0,
                    departureTime = LocalTime.of(6, 30),
                    route = null,
                    stop = null,
                ),
                CommuteShuttleTimetable(
                    seq = 2,
                    routeName = "TEST_ROUTE_1",
                    stopName = "TEST_STOP_2",
                    order = 1,
                    departureTime = LocalTime.of(6, 40),
                    route = null,
                    stop = null,
                ),
            ),
        ).whenever(service).getTimetableByRouteName("TEST_ROUTE_1")
        mockMvc
            .perform(get("/api/v1/commute-shuttle/route/TEST_ROUTE_1/timetable"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result").isArray)
            .andExpect(jsonPath("$.result.length()").value(2))
            .andExpect(jsonPath("$.result[0].seq").value(1))
            .andExpect(jsonPath("$.result[0].routeID").value("TEST_ROUTE_1"))
            .andExpect(jsonPath("$.result[0].stopID").value("TEST_STOP_1"))
            .andExpect(jsonPath("$.result[0].order").value(0))
            .andExpect(jsonPath("$.result[1].seq").value(2))
            .andExpect(jsonPath("$.result[1].routeID").value("TEST_ROUTE_1"))
            .andExpect(jsonPath("$.result[1].stopID").value("TEST_STOP_2"))
            .andExpect(jsonPath("$.result[1].order").value(1))
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 목록 조회 - 존재하지 않는 노선")
    @WithCustomMockUser(username = "test_user")
    fun testGetShuttleTimetableByRouteNotFound() {
        doThrow(ShuttleRouteNotFoundException()).whenever(service).getTimetableByRouteName("TEST_ROUTE_1")
        mockMvc
            .perform(get("/api/v1/commute-shuttle/route/TEST_ROUTE_1/timetable"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 목록 조회 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testGetShuttleTimetableByRouteOtherException() {
        doThrow(RuntimeException()).whenever(service).getTimetableByRouteName("TEST_ROUTE_1")
        mockMvc
            .perform(get("/api/v1/commute-shuttle/route/TEST_ROUTE_1/timetable"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 생성")
    @WithCustomMockUser(username = "test_user")
    fun testCreateShuttleTimetable() {
        doReturn(
            CommuteShuttleTimetable(
                seq = 1,
                routeName = "TEST_ROUTE_1",
                stopName = "TEST_STOP_1",
                order = 0,
                departureTime = LocalTime.of(6, 30),
                route = null,
                stop = null,
            ),
        ).whenever(service).createTimetable(
            "TEST_ROUTE_1",
            ShuttleTimetableRequest(
                routeName = "TEST_ROUTE_1",
                stopID = "TEST_STOP_1",
                order = 0,
                time = "06:30:00",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/commute-shuttle/route/TEST_ROUTE_1/timetable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttleTimetableRequest(
                                routeName = "TEST_ROUTE_1",
                                stopID = "TEST_STOP_1",
                                order = 0,
                                time = "06:30:00",
                            ),
                        ),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.routeID").value("TEST_ROUTE_1"))
            .andExpect(jsonPath("$.stopID").value("TEST_STOP_1"))
            .andExpect(jsonPath("$.order").value(0))
            .andExpect(jsonPath("$.time").value("06:30:00"))
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 생성 - 존재하지 않는 노선")
    @WithCustomMockUser(username = "test_user")
    fun testCreateShuttleTimetableRouteNotFound() {
        doThrow(ShuttleRouteNotFoundException()).whenever(service).createTimetable(
            "TEST_ROUTE_1",
            ShuttleTimetableRequest(
                routeName = "TEST_ROUTE_1",
                stopID = "TEST_STOP_1",
                order = 0,
                time = "06:30:00",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/commute-shuttle/route/TEST_ROUTE_1/timetable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttleTimetableRequest(
                                routeName = "TEST_ROUTE_1",
                                stopID = "TEST_STOP_1",
                                order = 0,
                                time = "06:30:00",
                            ),
                        ),
                    ),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 생성 - 존재하지 않는 정류장")
    @WithCustomMockUser(username = "test_user")
    fun testCreateShuttleTimetableStopNotFound() {
        doThrow(ShuttleStopNotFoundException()).whenever(service).createTimetable(
            "TEST_ROUTE_1",
            ShuttleTimetableRequest(
                routeName = "TEST_ROUTE_1",
                stopID = "TEST_STOP_1",
                order = 0,
                time = "06:30:00",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/commute-shuttle/route/TEST_ROUTE_1/timetable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttleTimetableRequest(
                                routeName = "TEST_ROUTE_1",
                                stopID = "TEST_STOP_1",
                                order = 0,
                                time = "06:30:00",
                            ),
                        ),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("SHUTTLE_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 생성 - 잘못된 시간 형식")
    @WithCustomMockUser(username = "test_user")
    fun testCreateShuttleTimetableInvalidTimeFormat() {
        doThrow(LocalTimeNotValidException()).whenever(service).createTimetable(
            "TEST_ROUTE_1",
            ShuttleTimetableRequest(
                routeName = "TEST_ROUTE_1",
                stopID = "TEST_STOP_1",
                order = 0,
                time = "25:00:00",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/commute-shuttle/route/TEST_ROUTE_1/timetable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttleTimetableRequest(
                                routeName = "TEST_ROUTE_1",
                                stopID = "TEST_STOP_1",
                                order = 0,
                                time = "25:00:00",
                            ),
                        ),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("INVALID_DATE_TIME_FORMAT"))
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 생성 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testCreateShuttleTimetableOtherException() {
        doThrow(RuntimeException()).whenever(service).createTimetable(
            "TEST_ROUTE_1",
            ShuttleTimetableRequest(
                routeName = "TEST_ROUTE_1",
                stopID = "TEST_STOP_1",
                order = 0,
                time = "06:30:00",
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/commute-shuttle/route/TEST_ROUTE_1/timetable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttleTimetableRequest(
                                routeName = "TEST_ROUTE_1",
                                stopID = "TEST_STOP_1",
                                order = 0,
                                time = "06:30:00",
                            ),
                        ),
                    ),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 항목 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetShuttleTimetableByRouteAndSeq() {
        doReturn(
            CommuteShuttleTimetable(
                seq = 1,
                routeName = "TEST_ROUTE_1",
                stopName = "TEST_STOP_1",
                order = 0,
                departureTime = LocalTime.of(6, 30),
                route = null,
                stop = null,
            ),
        ).whenever(service).getShuttleTimetableByRouteNameAndSeq("TEST_ROUTE_1", 1)
        mockMvc
            .perform(get("/api/v1/commute-shuttle/route/TEST_ROUTE_1/timetable/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.routeID").value("TEST_ROUTE_1"))
            .andExpect(jsonPath("$.stopID").value("TEST_STOP_1"))
            .andExpect(jsonPath("$.order").value(0))
            .andExpect(jsonPath("$.time").value("06:30:00"))
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 항목 조회 - 존재하지 않는 노선")
    @WithCustomMockUser(username = "test_user")
    fun testGetShuttleTimetableByRouteAndSeqRouteNotFound() {
        doThrow(ShuttleRouteNotFoundException()).whenever(service).getShuttleTimetableByRouteNameAndSeq("TEST_ROUTE_1", 1)
        mockMvc
            .perform(get("/api/v1/commute-shuttle/route/TEST_ROUTE_1/timetable/1"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 항목 조회 - 존재하지 않는 시간표")
    @WithCustomMockUser(username = "test_user")
    fun testGetShuttleTimetableByRouteAndSeqTimetableNotFound() {
        doThrow(ShuttleTimetableNotFoundException()).whenever(service).getShuttleTimetableByRouteNameAndSeq("TEST_ROUTE_1", 1)
        mockMvc
            .perform(get("/api/v1/commute-shuttle/route/TEST_ROUTE_1/timetable/1"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("SHUTTLE_TIMETABLE_NOT_FOUND"))
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 항목 조회 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testGetShuttleTimetableByRouteAndSeqOtherException() {
        doThrow(RuntimeException()).whenever(service).getShuttleTimetableByRouteNameAndSeq("TEST_ROUTE_1", 1)
        mockMvc
            .perform(get("/api/v1/commute-shuttle/route/TEST_ROUTE_1/timetable/1"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 항목 수정")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateShuttleTimetableByRouteAndSeq() {
        doReturn(
            CommuteShuttleTimetable(
                seq = 1,
                routeName = "TEST_ROUTE_1",
                stopName = "TEST_STOP_1",
                order = 0,
                departureTime = LocalTime.of(6, 30),
                route = null,
                stop = null,
            ),
        ).whenever(service).updateTimetable(
            "TEST_ROUTE_1",
            1,
            ShuttleTimetableRequest(
                routeName = "TEST_ROUTE_1",
                stopID = "TEST_STOP_1",
                order = 0,
                time = "06:30:00",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/commute-shuttle/route/TEST_ROUTE_1/timetable/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttleTimetableRequest(
                                routeName = "TEST_ROUTE_1",
                                stopID = "TEST_STOP_1",
                                order = 0,
                                time = "06:30:00",
                            ),
                        ),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.routeID").value("TEST_ROUTE_1"))
            .andExpect(jsonPath("$.stopID").value("TEST_STOP_1"))
            .andExpect(jsonPath("$.order").value(0))
            .andExpect(jsonPath("$.time").value("06:30:00"))
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 항목 수정 - 존재하지 않는 노선")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateShuttleTimetableByRouteAndSeqRouteNotFound() {
        doThrow(ShuttleRouteNotFoundException()).whenever(service).updateTimetable(
            "TEST_ROUTE_1",
            1,
            ShuttleTimetableRequest(
                routeName = "TEST_ROUTE_1",
                stopID = "TEST_STOP_1",
                order = 0,
                time = "06:30:00",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/commute-shuttle/route/TEST_ROUTE_1/timetable/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttleTimetableRequest(
                                routeName = "TEST_ROUTE_1",
                                stopID = "TEST_STOP_1",
                                order = 0,
                                time = "06:30:00",
                            ),
                        ),
                    ),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 항목 수정 - 존재하지 않는 시간표")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateShuttleTimetableByRouteAndSeqTimetableNotFound() {
        doThrow(ShuttleTimetableNotFoundException()).whenever(service).updateTimetable(
            "TEST_ROUTE_1",
            1,
            ShuttleTimetableRequest(
                routeName = "TEST_ROUTE_1",
                stopID = "TEST_STOP_1",
                order = 0,
                time = "06:30:00",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/commute-shuttle/route/TEST_ROUTE_1/timetable/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttleTimetableRequest(
                                routeName = "TEST_ROUTE_1",
                                stopID = "TEST_STOP_1",
                                order = 0,
                                time = "06:30:00",
                            ),
                        ),
                    ),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("SHUTTLE_TIMETABLE_NOT_FOUND"))
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 항목 수정 - 존재하지 않는 정류장")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateShuttleTimetableByRouteAndSeqStopNotFound() {
        doThrow(ShuttleStopNotFoundException()).whenever(service).updateTimetable(
            "TEST_ROUTE_1",
            1,
            ShuttleTimetableRequest(
                routeName = "TEST_ROUTE_1",
                stopID = "TEST_STOP_1",
                order = 0,
                time = "06:30:00",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/commute-shuttle/route/TEST_ROUTE_1/timetable/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttleTimetableRequest(
                                routeName = "TEST_ROUTE_1",
                                stopID = "TEST_STOP_1",
                                order = 0,
                                time = "06:30:00",
                            ),
                        ),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("SHUTTLE_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 항목 수정 - 잘못된 시간 형식")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateShuttleTimetableByRouteAndSeqInvalidTimeFormat() {
        doThrow(LocalTimeNotValidException()).whenever(service).updateTimetable(
            "TEST_ROUTE_1",
            1,
            ShuttleTimetableRequest(
                routeName = "TEST_ROUTE_1",
                stopID = "TEST_STOP_1",
                order = 0,
                time = "25:00:00",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/commute-shuttle/route/TEST_ROUTE_1/timetable/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttleTimetableRequest(
                                routeName = "TEST_ROUTE_1",
                                stopID = "TEST_STOP_1",
                                order = 0,
                                time = "25:00:00",
                            ),
                        ),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("INVALID_DATE_TIME_FORMAT"))
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 항목 수정 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateShuttleTimetableByRouteAndSeqOtherException() {
        doThrow(RuntimeException()).whenever(service).updateTimetable(
            "TEST_ROUTE_1",
            1,
            ShuttleTimetableRequest(
                routeName = "TEST_ROUTE_1",
                stopID = "TEST_STOP_1",
                order = 0,
                time = "06:30:00",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/commute-shuttle/route/TEST_ROUTE_1/timetable/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            ShuttleTimetableRequest(
                                routeName = "TEST_ROUTE_1",
                                stopID = "TEST_STOP_1",
                                order = 0,
                                time = "06:30:00",
                            ),
                        ),
                    ),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 항목 삭제")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteShuttleTimetableByRouteAndSeq() {
        mockMvc
            .perform(
                delete("/api/v1/commute-shuttle/route/TEST_ROUTE_1/timetable/1"),
            ).andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 항목 삭제 - 존재하지 않는 노선")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteShuttleTimetableByRouteAndSeqRouteNotFound() {
        doThrow(ShuttleRouteNotFoundException()).whenever(service).deleteTimetable(1)
        mockMvc
            .perform(
                delete("/api/v1/commute-shuttle/route/TEST_ROUTE_1/timetable/1"),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 항목 삭제 - 존재하지 않는 시간표")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteShuttleTimetableByRouteAndSeqTimetableNotFound() {
        doThrow(ShuttleTimetableNotFoundException()).whenever(service).deleteTimetable(1)
        mockMvc
            .perform(
                delete("/api/v1/commute-shuttle/route/TEST_ROUTE_1/timetable/1"),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("SHUTTLE_TIMETABLE_NOT_FOUND"))
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 항목 삭제 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteShuttleTimetableByRouteAndSeqOtherException() {
        doThrow(RuntimeException()).whenever(service).deleteTimetable(1)
        mockMvc
            .perform(
                delete("/api/v1/commute-shuttle/route/TEST_ROUTE_1/timetable/1"),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("통학버스 정류장 전체 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetAllShuttleStops() {
        doReturn(
            listOf(
                CommuteShuttleStop(
                    name = "TEST_STOP_1",
                    description = "테스트 정류장 1",
                    latitude = 37.123456,
                    longitude = 127.123456,
                    timetable = mutableListOf(),
                ),
                CommuteShuttleStop(
                    name = "TEST_STOP_2",
                    description = "테스트 정류장 2",
                    latitude = 37.654321,
                    longitude = 127.654321,
                    timetable = mutableListOf(),
                ),
            ),
        ).whenever(service).getAllStops()
        mockMvc
            .perform(get("/api/v1/commute-shuttle/stop"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result").isArray)
            .andExpect(jsonPath("$.result.length()").value(2))
            .andExpect(jsonPath("$.result[0].name").value("TEST_STOP_1"))
            .andExpect(jsonPath("$.result[0].description").value("테스트 정류장 1"))
            .andExpect(jsonPath("$.result[0].latitude").value(37.123456))
            .andExpect(jsonPath("$.result[0].longitude").value(127.123456))
            .andExpect(jsonPath("$.result[1].name").value("TEST_STOP_2"))
            .andExpect(jsonPath("$.result[1].description").value("테스트 정류장 2"))
            .andExpect(jsonPath("$.result[1].latitude").value(37.654321))
            .andExpect(jsonPath("$.result[1].longitude").value(127.654321))
    }

    @Test
    @DisplayName("통학버스 정류장 생성")
    @WithCustomMockUser(username = "test_user")
    fun testCreateShuttleStop() {
        doReturn(
            CommuteShuttleStop(
                name = "TEST_STOP_1",
                description = "테스트 정류장 1",
                latitude = 37.123456,
                longitude = 127.123456,
                timetable = mutableListOf(),
            ),
        ).whenever(service).createStop(
            CreateShuttleStopRequest(
                name = "TEST_STOP_1",
                description = "테스트 정류장 1",
                latitude = 37.123456,
                longitude = 127.123456,
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/commute-shuttle/stop")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            CreateShuttleStopRequest(
                                name = "TEST_STOP_1",
                                description = "테스트 정류장 1",
                                latitude = 37.123456,
                                longitude = 127.123456,
                            ),
                        ),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("TEST_STOP_1"))
            .andExpect(jsonPath("$.description").value("테스트 정류장 1"))
            .andExpect(jsonPath("$.latitude").value(37.123456))
            .andExpect(jsonPath("$.longitude").value(127.123456))
    }

    @Test
    @DisplayName("통학버스 정류장 생성 - 이미 존재하는 정류장")
    @WithCustomMockUser(username = "test_user")
    fun testCreateShuttleStopAlreadyExists() {
        doThrow(DuplicateShuttleStopException()).whenever(service).createStop(
            CreateShuttleStopRequest(
                name = "TEST_STOP_1",
                description = "테스트 정류장 1",
                latitude = 37.123456,
                longitude = 127.123456,
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/commute-shuttle/stop")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            CreateShuttleStopRequest(
                                name = "TEST_STOP_1",
                                description = "테스트 정류장 1",
                                latitude = 37.123456,
                                longitude = 127.123456,
                            ),
                        ),
                    ),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("DUPLICATE_SHUTTLE_STOP"))
    }

    @Test
    @DisplayName("통학버스 정류장 생성 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testCreateShuttleStopOtherException() {
        doThrow(RuntimeException()).whenever(service).createStop(
            CreateShuttleStopRequest(
                name = "TEST_STOP_1",
                description = "테스트 정류장 1",
                latitude = 37.123456,
                longitude = 127.123456,
            ),
        )
        mockMvc
            .perform(
                post("/api/v1/commute-shuttle/stop")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            CreateShuttleStopRequest(
                                name = "TEST_STOP_1",
                                description = "테스트 정류장 1",
                                latitude = 37.123456,
                                longitude = 127.123456,
                            ),
                        ),
                    ),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("통학버스 정류장 항목 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetShuttleStopByName() {
        doReturn(
            CommuteShuttleStop(
                name = "TEST_STOP_1",
                description = "테스트 정류장 1",
                latitude = 37.123456,
                longitude = 127.123456,
                timetable = mutableListOf(),
            ),
        ).whenever(service).getStopByName("TEST_STOP_1")
        mockMvc
            .perform(get("/api/v1/commute-shuttle/stop/TEST_STOP_1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("TEST_STOP_1"))
            .andExpect(jsonPath("$.description").value("테스트 정류장 1"))
            .andExpect(jsonPath("$.latitude").value(37.123456))
            .andExpect(jsonPath("$.longitude").value(127.123456))
    }

    @Test
    @DisplayName("통학버스 정류장 항목 조회 - 존재하지 않는 정류장")
    @WithCustomMockUser(username = "test_user")
    fun testGetShuttleStopByNameNotFound() {
        doThrow(ShuttleStopNotFoundException()).whenever(service).getStopByName("TEST_STOP_1")
        mockMvc
            .perform(get("/api/v1/commute-shuttle/stop/TEST_STOP_1"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("SHUTTLE_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("통학버스 정류장 항목 조회 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testGetShuttleStopByNameOtherException() {
        doThrow(RuntimeException()).whenever(service).getStopByName("TEST_STOP_1")
        mockMvc
            .perform(get("/api/v1/commute-shuttle/stop/TEST_STOP_1"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("통학버스 정류장 수정")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateShuttleStop() {
        doReturn(
            CommuteShuttleStop(
                name = "TEST_STOP_1",
                description = "테스트 정류장 1 - 수정됨",
                latitude = 37.123456,
                longitude = 127.123456,
                timetable = mutableListOf(),
            ),
        ).whenever(service).updateStop(
            "TEST_STOP_1",
            UpdateShuttleStopRequest(
                description = "테스트 정류장 1 - 수정됨",
                latitude = 37.123456,
                longitude = 127.123456,
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/commute-shuttle/stop/TEST_STOP_1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            UpdateShuttleStopRequest(
                                description = "테스트 정류장 1 - 수정됨",
                                latitude = 37.123456,
                                longitude = 127.123456,
                            ),
                        ),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("TEST_STOP_1"))
            .andExpect(jsonPath("$.description").value("테스트 정류장 1 - 수정됨"))
            .andExpect(jsonPath("$.latitude").value(37.123456))
            .andExpect(jsonPath("$.longitude").value(127.123456))
    }

    @Test
    @DisplayName("통학버스 정류장 수정 - 존재하지 않는 정류장")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateShuttleStopNotFound() {
        doThrow(ShuttleStopNotFoundException()).whenever(service).updateStop(
            "TEST_STOP_1",
            UpdateShuttleStopRequest(
                description = "테스트 정류장 1 - 수정됨",
                latitude = 37.123456,
                longitude = 127.123456,
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/commute-shuttle/stop/TEST_STOP_1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            UpdateShuttleStopRequest(
                                description = "테스트 정류장 1 - 수정됨",
                                latitude = 37.123456,
                                longitude = 127.123456,
                            ),
                        ),
                    ),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("SHUTTLE_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("통학버스 정류장 수정 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateShuttleStopOtherException() {
        doThrow(RuntimeException()).whenever(service).updateStop(
            "TEST_STOP_1",
            UpdateShuttleStopRequest(
                description = "테스트 정류장 1 - 수정됨",
                latitude = 37.123456,
                longitude = 127.123456,
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/commute-shuttle/stop/TEST_STOP_1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            UpdateShuttleStopRequest(
                                description = "테스트 정류장 1 - 수정됨",
                                latitude = 37.123456,
                                longitude = 127.123456,
                            ),
                        ),
                    ),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("통학버스 정류장 삭제")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteShuttleStop() {
        mockMvc
            .perform(
                delete("/api/v1/commute-shuttle/stop/TEST_STOP_1"),
            ).andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("통학버스 정류장 삭제 - 존재하지 않는 정류장")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteShuttleStopNotFound() {
        doThrow(ShuttleStopNotFoundException()).whenever(service).deleteStop("TEST_STOP_1")
        mockMvc
            .perform(
                delete("/api/v1/commute-shuttle/stop/TEST_STOP_1"),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("SHUTTLE_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("통학버스 정류장 삭제 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteShuttleStopOtherException() {
        doThrow(RuntimeException()).whenever(service).deleteStop("TEST_STOP_1")
        mockMvc
            .perform(
                delete("/api/v1/commute-shuttle/stop/TEST_STOP_1"),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("통학버스 시간표 전체 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetAllShuttleTimetables() {
        doReturn(
            listOf(
                CommuteShuttleTimetable(
                    seq = 1,
                    routeName = "TEST_ROUTE_1",
                    stopName = "TEST_STOP_1",
                    order = 0,
                    departureTime = LocalTime.of(6, 30),
                    route = null,
                    stop = null,
                ),
                CommuteShuttleTimetable(
                    seq = 2,
                    routeName = "TEST_ROUTE_1",
                    stopName = "TEST_STOP_2",
                    order = 1,
                    departureTime = LocalTime.of(6, 40),
                    route = null,
                    stop = null,
                ),
            ),
        ).whenever(service).getAllTimetables()
        mockMvc
            .perform(get("/api/v1/commute-shuttle/timetable"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result").isArray)
            .andExpect(jsonPath("$.result.length()").value(2))
            .andExpect(jsonPath("$.result[0].seq").value(1))
            .andExpect(jsonPath("$.result[0].routeID").value("TEST_ROUTE_1"))
            .andExpect(jsonPath("$.result[0].stopID").value("TEST_STOP_1"))
            .andExpect(jsonPath("$.result[0].order").value(0))
            .andExpect(jsonPath("$.result[1].seq").value(2))
            .andExpect(jsonPath("$.result[1].routeID").value("TEST_ROUTE_1"))
            .andExpect(jsonPath("$.result[1].stopID").value("TEST_STOP_2"))
            .andExpect(jsonPath("$.result[1].order").value(1))
    }
}
