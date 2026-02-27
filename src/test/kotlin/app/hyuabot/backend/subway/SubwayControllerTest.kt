package app.hyuabot.backend.subway

import app.hyuabot.backend.database.entity.SubwayRealtime
import app.hyuabot.backend.database.entity.SubwayRoute
import app.hyuabot.backend.database.entity.SubwayRouteStation
import app.hyuabot.backend.database.entity.SubwayTimetable
import app.hyuabot.backend.database.exception.DurationNotValidException
import app.hyuabot.backend.database.exception.LocalTimeNotValidException
import app.hyuabot.backend.security.WithCustomMockUser
import app.hyuabot.backend.subway.domain.CreateSubwayRouteRequest
import app.hyuabot.backend.subway.domain.CreateSubwayStationRequest
import app.hyuabot.backend.subway.domain.SubwayTimetableRequest
import app.hyuabot.backend.subway.domain.UpdateSubwayRouteRequest
import app.hyuabot.backend.subway.domain.UpdateSubwayStationRequest
import app.hyuabot.backend.subway.exception.DuplicateSubwayRouteException
import app.hyuabot.backend.subway.exception.DuplicateSubwayStationException
import app.hyuabot.backend.subway.exception.DuplicateSubwayTimetableException
import app.hyuabot.backend.subway.exception.SubwayRouteNotFoundException
import app.hyuabot.backend.subway.exception.SubwayStartStationNotFoundException
import app.hyuabot.backend.subway.exception.SubwayStationNotFoundException
import app.hyuabot.backend.subway.exception.SubwayTerminalStationNotFoundException
import app.hyuabot.backend.subway.exception.SubwayTimetableNotFoundException
import app.hyuabot.backend.subway.service.SubwayService
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
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubwayControllerTest {
    @MockitoBean
    private lateinit var service: SubwayService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DisplayName("전철 노선 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetSubwayRoutes() {
        doReturn(
            listOf(
                SubwayRoute(
                    id = 1,
                    name = "1호선",
                    station = emptyList(),
                ),
                SubwayRoute(
                    id = 2,
                    name = "2호선",
                    station = emptyList(),
                ),
            ),
        ).whenever(service).getSubwayRoutes()
        mockMvc
            .perform(get("/api/v1/subway/route"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.result").isArray)
            .andExpect(jsonPath("$.result[0].id").value(1))
            .andExpect(jsonPath("$.result[0].name").value("1호선"))
            .andExpect(jsonPath("$.result[1].id").value(2))
            .andExpect(jsonPath("$.result[1].name").value("2호선"))
    }

    @Test
    @DisplayName("전철 노선 생성")
    @WithCustomMockUser(username = "test_user")
    fun testCreateSubwayRoute() {
        val newRoute =
            SubwayRoute(
                id = 3,
                name = "3호선",
                station = emptyList(),
            )
        doReturn(newRoute).whenever(service).createSubwayRoute(
            CreateSubwayRouteRequest(
                id = 3,
                name = "3호선",
            ),
        )
        val payload =
            CreateSubwayRouteRequest(
                id = 3,
                name = "3호선",
            )
        mockMvc
            .perform(
                post("/api/v1/subway/route")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(3))
            .andExpect(jsonPath("$.name").value("3호선"))
    }

    @Test
    @DisplayName("전철 노선 생성 - 중복된 ID")
    @WithCustomMockUser(username = "test_user")
    fun testCreateSubwayRouteDuplicateID() {
        whenever(
            service.createSubwayRoute(
                CreateSubwayRouteRequest(
                    id = 1,
                    name = "1호선",
                ),
            ),
        ).thenThrow(DuplicateSubwayRouteException())
        val payload =
            CreateSubwayRouteRequest(
                id = 1,
                name = "1호선",
            )
        mockMvc
            .perform(
                post("/api/v1/subway/route")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isConflict)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("DUPLICATE_SUBWAY_ROUTE"))
    }

    @Test
    @DisplayName("전철 노선 생성 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testCreateSubwayRouteOtherException() {
        doThrow(RuntimeException())
            .whenever(
                service,
            ).createSubwayRoute(
                CreateSubwayRouteRequest(
                    id = 1,
                    name = "1호선",
                ),
            )
        val payload =
            CreateSubwayRouteRequest(
                id = 1,
                name = "1호선",
            )
        mockMvc
            .perform(
                post("/api/v1/subway/route")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("전철 노선 항목 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetSubwayRouteById() {
        doReturn(
            SubwayRoute(
                id = 1,
                name = "1호선",
                station = emptyList(),
            ),
        ).whenever(service).getSubwayRouteById(1)
        mockMvc
            .perform(get("/api/v1/subway/route/1"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("1호선"))
    }

    @Test
    @DisplayName("전철 노선 항목 조회 - 없는 ID")
    @WithCustomMockUser(username = "test_user")
    fun testGetSubwayRouteByIdNotFound() {
        whenever(service.getSubwayRouteById(99)).thenThrow(SubwayRouteNotFoundException())
        mockMvc
            .perform(get("/api/v1/subway/route/99"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SUBWAY_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("전철 노선 항목 조회 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testGetSubwayRouteByIdOtherException() {
        doThrow(RuntimeException())
            .whenever(service)
            .getSubwayRouteById(1)
        mockMvc
            .perform(get("/api/v1/subway/route/1"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("전철 노선 항목 수정")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateSubwayRoute() {
        val updatedRoute =
            SubwayRoute(
                id = 1,
                name = "1호선-수정",
                station = emptyList(),
            )
        val payload = mapOf("name" to "1호선-수정")
        doReturn(updatedRoute).whenever(service).updateSubwayRoute(
            1,
            UpdateSubwayRouteRequest(name = "1호선-수정"),
        )
        mockMvc
            .perform(
                put("/api/v1/subway/route/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("1호선-수정"))
    }

    @Test
    @DisplayName("전철 노선 항목 수정 - 없는 ID")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateSubwayRouteNotFound() {
        val payload =
            UpdateSubwayRouteRequest(
                name = "1호선-수정",
            )
        whenever(
            service.updateSubwayRoute(
                99,
                UpdateSubwayRouteRequest(name = "1호선-수정"),
            ),
        ).thenThrow(SubwayRouteNotFoundException())
        mockMvc
            .perform(
                put("/api/v1/subway/route/99")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SUBWAY_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("전철 노선 항목 수정 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateSubwayRouteOtherException() {
        val payload =
            UpdateSubwayRouteRequest(
                name = "1호선-수정",
            )
        doThrow(RuntimeException())
            .whenever(service)
            .updateSubwayRoute(
                1,
                UpdateSubwayRouteRequest(name = "1호선-수정"),
            )
        mockMvc
            .perform(
                put("/api/v1/subway/route/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("전철 노선 항목 삭제")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteSubwayRoute() {
        mockMvc
            .perform(
                delete("/api/v1/subway/route/1"),
            ).andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("전철 노선 항목 삭제 - 없는 ID")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteSubwayRouteNotFound() {
        whenever(service.deleteSubwayRoute(99)).thenThrow(SubwayRouteNotFoundException())
        mockMvc
            .perform(
                delete("/api/v1/subway/route/99"),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SUBWAY_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("전철 노선 항목 삭제 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteSubwayRouteOtherException() {
        doThrow(RuntimeException())
            .whenever(service)
            .deleteSubwayRoute(1)
        mockMvc
            .perform(
                delete("/api/v1/subway/route/1"),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("전철 역 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetAllStations() {
        doReturn(
            listOf(
                SubwayRouteStation(
                    id = "K401",
                    routeID = 1,
                    name = "역1",
                    order = 1,
                    cumulativeTime = Duration.ofMinutes(10),
                    route = null,
                    stationName = null,
                    realtime = emptyList(),
                    timetable = emptyList(),
                ),
                SubwayRouteStation(
                    id = "K402",
                    routeID = 1,
                    name = "역2",
                    order = 2,
                    cumulativeTime = Duration.ofMinutes(20),
                    route = null,
                    stationName = null,
                    realtime = emptyList(),
                    timetable = emptyList(),
                ),
            ),
        ).whenever(service).getAllStations()
        mockMvc
            .perform(get("/api/v1/subway/station"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.result").isArray)
            .andExpect(jsonPath("$.result[0].id").value("K401"))
            .andExpect(jsonPath("$.result[0].name").value("역1"))
            .andExpect(jsonPath("$.result[1].id").value("K402"))
            .andExpect(jsonPath("$.result[1].name").value("역2"))
    }

    @Test
    @DisplayName("전철 역 생성")
    @WithCustomMockUser(username = "test_user")
    fun testCreateSubwayStation() {
        val newStation =
            SubwayRouteStation(
                id = "K403",
                routeID = 1,
                name = "역3",
                order = 3,
                cumulativeTime = Duration.ofMinutes(30),
                route = null,
                stationName = null,
                realtime = emptyList(),
                timetable = emptyList(),
            )
        val payload =
            CreateSubwayStationRequest(
                id = "K403",
                routeID = 1,
                name = "역3",
                order = 3,
                cumulativeTime = "00:30:00",
            )
        doReturn(newStation).whenever(service).createStation(payload)
        mockMvc
            .perform(
                post("/api/v1/subway/station")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value("K403"))
            .andExpect(jsonPath("$.name").value("역3"))
    }

    @Test
    @DisplayName("전철 역 생성 - 시점으로부터 소요시간 형식 오류")
    @WithCustomMockUser(username = "test_user")
    fun testCreateSubwayStationInvalidDuration() {
        val payload =
            CreateSubwayStationRequest(
                id = "K403",
                routeID = 1,
                name = "역3",
                order = 3,
                cumulativeTime = "30분",
            )
        doThrow(DurationNotValidException()).whenever(service).createStation(payload)
        mockMvc
            .perform(
                post("/api/v1/subway/station")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INVALID_TIME_FORMAT"))
    }

    @Test
    @DisplayName("전철 역 생성 - 중복된 ID")
    @WithCustomMockUser(username = "test_user")
    fun testCreateSubwayStationDuplicateID() {
        val payload =
            CreateSubwayStationRequest(
                id = "K401",
                routeID = 1,
                name = "역1",
                order = 1,
                cumulativeTime = "00:10:00",
            )
        doThrow(DuplicateSubwayStationException()).whenever(service).createStation(payload)
        mockMvc
            .perform(
                post("/api/v1/subway/station")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isConflict)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("DUPLICATED_STATION_ID"))
    }

    @Test
    @DisplayName("전철 역 생성 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testCreateSubwayStationOtherException() {
        val payload =
            CreateSubwayStationRequest(
                id = "K403",
                routeID = 1,
                name = "역3",
                order = 3,
                cumulativeTime = "00:30:00",
            )
        doThrow(RuntimeException()).whenever(service).createStation(payload)
        mockMvc
            .perform(
                post("/api/v1/subway/station")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("전철 역 항목 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetSubwayStationById() {
        doReturn(
            SubwayRouteStation(
                id = "K401",
                routeID = 1,
                name = "역1",
                order = 1,
                cumulativeTime = Duration.ofMinutes(10),
                route = null,
                stationName = null,
                realtime = emptyList(),
                timetable = emptyList(),
            ),
        ).whenever(service).getStationById("K401")
        mockMvc
            .perform(get("/api/v1/subway/station/K401"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value("K401"))
            .andExpect(jsonPath("$.name").value("역1"))
    }

    @Test
    @DisplayName("전철 역 항목 조회 - 없는 ID")
    @WithCustomMockUser(username = "test_user")
    fun testGetSubwayStationByIdNotFound() {
        doThrow(SubwayStationNotFoundException()).whenever(service).getStationById("K499")
        mockMvc
            .perform(get("/api/v1/subway/station/K499"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SUBWAY_STATION_NOT_FOUND"))
    }

    @Test
    @DisplayName("전철 역 항목 조회 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testGetSubwayStationByIdOtherException() {
        doThrow(RuntimeException()).whenever(service).getStationById("K401")
        mockMvc
            .perform(get("/api/v1/subway/station/K401"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("전철 역 항목 수정")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateSubwayStation() {
        val updatedStation =
            SubwayRouteStation(
                id = "K401",
                routeID = 1,
                name = "역1-수정",
                order = 1,
                cumulativeTime = Duration.ofMinutes(15),
                route = null,
                stationName = null,
                realtime = emptyList(),
                timetable = emptyList(),
            )
        val payload =
            mapOf(
                "routeID" to 1,
                "name" to "역1-수정",
                "order" to 1,
                "cumulativeTime" to "00:15:00",
            )
        doReturn(updatedStation).whenever(service).updateStation(
            "K401",
            UpdateSubwayStationRequest(
                routeID = 1,
                name = "역1-수정",
                order = 1,
                cumulativeTime = "00:15:00",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/subway/station/K401")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value("K401"))
            .andExpect(jsonPath("$.name").value("역1-수정"))
    }

    @Test
    @DisplayName("전철 역 항목 수정 - 없는 ID")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateSubwayStationNotFound() {
        val payload =
            UpdateSubwayStationRequest(
                routeID = 1,
                name = "역1-수정",
                order = 1,
                cumulativeTime = "00:15:00",
            )
        doThrow(SubwayStationNotFoundException()).whenever(service).updateStation(
            "K499",
            UpdateSubwayStationRequest(
                routeID = 1,
                name = "역1-수정",
                order = 1,
                cumulativeTime = "00:15:00",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/subway/station/K499")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SUBWAY_STATION_NOT_FOUND"))
    }

    @Test
    @DisplayName("전철 역 항목 수정 - 시점으로부터 소요시간 형식 오류")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateSubwayStationInvalidDuration() {
        val payload =
            UpdateSubwayStationRequest(
                routeID = 1,
                name = "역1-수정",
                order = 1,
                cumulativeTime = "15분",
            )
        doThrow(DurationNotValidException()).whenever(service).updateStation(
            "K401",
            UpdateSubwayStationRequest(
                routeID = 1,
                name = "역1-수정",
                order = 1,
                cumulativeTime = "15분",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/subway/station/K401")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INVALID_TIME_FORMAT"))
    }

    @Test
    @DisplayName("전철 역 항목 수정 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateSubwayStationOtherException() {
        val payload =
            UpdateSubwayStationRequest(
                routeID = 1,
                name = "역1-수정",
                order = 1,
                cumulativeTime = "00:15:00",
            )
        doThrow(RuntimeException()).whenever(service).updateStation(
            "K401",
            UpdateSubwayStationRequest(
                routeID = 1,
                name = "역1-수정",
                order = 1,
                cumulativeTime = "00:15:00",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/subway/station/K401")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("전철 역 항목 삭제")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteSubwayStation() {
        mockMvc
            .perform(
                delete("/api/v1/subway/station/K401"),
            ).andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("전철 역 항목 삭제 - 없는 ID")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteSubwayStationNotFound() {
        doThrow(SubwayStationNotFoundException()).whenever(service).deleteStation("K499")
        mockMvc
            .perform(
                delete("/api/v1/subway/station/K499"),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SUBWAY_STATION_NOT_FOUND"))
    }

    @Test
    @DisplayName("전철 역 항목 삭제 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteSubwayStationOtherException() {
        doThrow(RuntimeException()).whenever(service).deleteStation("K401")
        mockMvc
            .perform(
                delete("/api/v1/subway/station/K401"),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetSubwayTimetableByStationId() {
        doReturn(
            listOf(
                SubwayTimetable(
                    seq = 1,
                    stationID = "K450",
                    startStationID = "K410",
                    terminalStationID = "K456",
                    departureTime = LocalTime.parse("09:00"),
                    weekday = "weekdays",
                    heading = "up",
                    station = null,
                    startStation = null,
                    terminalStation = null,
                ),
            ),
        ).whenever(service).getTimetablesByStationID("K450")
        mockMvc
            .perform(get("/api/v1/subway/station/K450/timetable"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.result").isArray)
            .andExpect(jsonPath("$.result[0].seq").value(1))
            .andExpect(jsonPath("$.result[0].stationID").value("K450"))
            .andExpect(jsonPath("$.result[0].startStationID").value("K410"))
            .andExpect(jsonPath("$.result[0].terminalStationID").value("K456"))
            .andExpect(jsonPath("$.result[0].departureTime").value("09:00:00"))
            .andExpect(jsonPath("$.result[0].weekday").value("weekdays"))
            .andExpect(jsonPath("$.result[0].direction").value("up"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 목록 조회 - 행선 필터링")
    @WithCustomMockUser(username = "test_user")
    fun testGetSubwayTimetableByStationIdWithHeading() {
        doReturn(
            listOf(
                SubwayTimetable(
                    seq = 1,
                    stationID = "K450",
                    startStationID = "K410",
                    terminalStationID = "K456",
                    departureTime = LocalTime.parse("09:00"),
                    weekday = "weekdays",
                    heading = "up",
                    station = null,
                    startStation = null,
                    terminalStation = null,
                ),
            ),
        ).whenever(service).getTimetablesByStationIDAndDirection("K450", "up")
        mockMvc
            .perform(get("/api/v1/subway/station/K450/timetable").param("direction", "up"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.result").isArray)
            .andExpect(jsonPath("$.result[0].seq").value(1))
            .andExpect(jsonPath("$.result[0].stationID").value("K450"))
            .andExpect(jsonPath("$.result[0].startStationID").value("K410"))
            .andExpect(jsonPath("$.result[0].terminalStationID").value("K456"))
            .andExpect(jsonPath("$.result[0].departureTime").value("09:00:00"))
            .andExpect(jsonPath("$.result[0].weekday").value("weekdays"))
            .andExpect(jsonPath("$.result[0].direction").value("up"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 목록 조회 - 요일 필터링")
    @WithCustomMockUser(username = "test_user")
    fun testGetSubwayTimetableByStationIdWithWeekday() {
        doReturn(
            listOf(
                SubwayTimetable(
                    seq = 1,
                    stationID = "K450",
                    startStationID = "K410",
                    terminalStationID = "K456",
                    departureTime = LocalTime.parse("09:00"),
                    weekday = "weekdays",
                    heading = "up",
                    station = null,
                    startStation = null,
                    terminalStation = null,
                ),
            ),
        ).whenever(service).getTimetablesByStationIDAndWeekday("K450", "weekdays")
        mockMvc
            .perform(get("/api/v1/subway/station/K450/timetable").param("weekday", "weekdays"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.result").isArray)
            .andExpect(jsonPath("$.result[0].seq").value(1))
            .andExpect(jsonPath("$.result[0].stationID").value("K450"))
            .andExpect(jsonPath("$.result[0].startStationID").value("K410"))
            .andExpect(jsonPath("$.result[0].terminalStationID").value("K456"))
            .andExpect(jsonPath("$.result[0].departureTime").value("09:00:00"))
            .andExpect(jsonPath("$.result[0].weekday").value("weekdays"))
            .andExpect(jsonPath("$.result[0].direction").value("up"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 목록 조회 - 요일 및 행선 필터링")
    @WithCustomMockUser(username = "test_user")
    fun testGetSubwayTimetableByStationIdWithWeekdayAndHeading() {
        doReturn(
            listOf(
                SubwayTimetable(
                    seq = 1,
                    stationID = "K450",
                    startStationID = "K410",
                    terminalStationID = "K456",
                    departureTime = LocalTime.parse("09:00"),
                    weekday = "weekdays",
                    heading = "up",
                    station = null,
                    startStation = null,
                    terminalStation = null,
                ),
            ),
        ).whenever(service).getTimetablesByStationIDAndDirectionAndWeekday("K450", "up", "weekdays")
        mockMvc
            .perform(
                get("/api/v1/subway/station/K450/timetable")
                    .param("direction", "up")
                    .param("weekday", "weekdays"),
            ).andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.result").isArray)
            .andExpect(jsonPath("$.result[0].seq").value(1))
            .andExpect(jsonPath("$.result[0].stationID").value("K450"))
            .andExpect(jsonPath("$.result[0].startStationID").value("K410"))
            .andExpect(jsonPath("$.result[0].terminalStationID").value("K456"))
            .andExpect(jsonPath("$.result[0].departureTime").value("09:00:00"))
            .andExpect(jsonPath("$.result[0].weekday").value("weekdays"))
            .andExpect(jsonPath("$.result[0].direction").value("up"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 목록 조회 - 없는 역 ID")
    @WithCustomMockUser(username = "test_user")
    fun testGetSubwayTimetableByStationIdNotFound() {
        doThrow(SubwayStationNotFoundException()).whenever(service).getTimetablesByStationID("K499")
        mockMvc
            .perform(get("/api/v1/subway/station/K499/timetable"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SUBWAY_STATION_NOT_FOUND"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 목록 조회 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testGetSubwayTimetableByStationIdOtherException() {
        doThrow(RuntimeException()).whenever(service).getTimetablesByStationID("K450")
        mockMvc
            .perform(get("/api/v1/subway/station/K450/timetable"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 항목 생성")
    @WithCustomMockUser(username = "test_user")
    fun testCreateSubwayTimetable() {
        val newTimetable =
            SubwayTimetable(
                seq = 1,
                stationID = "K450",
                startStationID = "K410",
                terminalStationID = "K456",
                departureTime = LocalTime.parse("09:00"),
                weekday = "weekdays",
                heading = "up",
                station = null,
                startStation = null,
                terminalStation = null,
            )
        val payload =
            SubwayTimetableRequest(
                startStationID = "K410",
                terminalStationID = "K456",
                departureTime = "09:00:00",
                weekday = "weekdays",
                direction = "up",
            )
        doReturn(newTimetable).whenever(service).createTimetable("K450", payload)
        mockMvc
            .perform(
                post("/api/v1/subway/station/K450/timetable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.stationID").value("K450"))
            .andExpect(jsonPath("$.startStationID").value("K410"))
            .andExpect(jsonPath("$.terminalStationID").value("K456"))
            .andExpect(jsonPath("$.departureTime").value("09:00:00"))
            .andExpect(jsonPath("$.weekday").value("weekdays"))
            .andExpect(jsonPath("$.direction").value("up"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 항목 생성 - 존재하지 않는 역 ID")
    @WithCustomMockUser(username = "test_user")
    fun testCreateSubwayTimetableStationNotFound() {
        val payload =
            SubwayTimetableRequest(
                startStationID = "K410",
                terminalStationID = "K456",
                departureTime = "09:00:00",
                weekday = "weekdays",
                direction = "up",
            )
        doThrow(SubwayStationNotFoundException()).whenever(service).createTimetable("K499", payload)
        mockMvc
            .perform(
                post("/api/v1/subway/station/K499/timetable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SUBWAY_STATION_NOT_FOUND"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 항목 생성 - 존재하지 않는 출발역 ID")
    @WithCustomMockUser(username = "test_user")
    fun testCreateSubwayTimetableStartStationNotFound() {
        val payload =
            SubwayTimetableRequest(
                startStationID = "K499",
                terminalStationID = "K456",
                departureTime = "09:00:00",
                weekday = "weekdays",
                direction = "up",
            )
        doThrow(SubwayStartStationNotFoundException()).whenever(service).createTimetable("K450", payload)
        mockMvc
            .perform(
                post("/api/v1/subway/station/K450/timetable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SUBWAY_START_STATION_NOT_FOUND"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 항목 생성 - 존재하지 않는 종착역 ID")
    @WithCustomMockUser(username = "test_user")
    fun testCreateSubwayTimetableTerminalStationNotFound() {
        val payload =
            SubwayTimetableRequest(
                startStationID = "K410",
                terminalStationID = "K499",
                departureTime = "09:00:00",
                weekday = "weekdays",
                direction = "up",
            )
        doThrow(SubwayTerminalStationNotFoundException()).whenever(service).createTimetable("K450", payload)
        mockMvc
            .perform(
                post("/api/v1/subway/station/K450/timetable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SUBWAY_TERMINAL_STATION_NOT_FOUND"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 항목 생성 - 출발 시간 형식 오류")
    @WithCustomMockUser(username = "test_user")
    fun testCreateSubwayTimetableInvalidTimeFormat() {
        val payload =
            SubwayTimetableRequest(
                startStationID = "K410",
                terminalStationID = "K456",
                departureTime = "9시",
                weekday = "weekdays",
                direction = "up",
            )
        doThrow(LocalTimeNotValidException()).whenever(service).createTimetable("K450", payload)
        mockMvc
            .perform(
                post("/api/v1/subway/station/K450/timetable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INVALID_TIME_FORMAT"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 항목 생성 - 중복된 시간표")
    @WithCustomMockUser(username = "test_user")
    fun testCreateSubwayTimetableDuplicate() {
        val payload =
            SubwayTimetableRequest(
                startStationID = "K410",
                terminalStationID = "K456",
                departureTime = "09:00:00",
                weekday = "weekdays",
                direction = "up",
            )
        doThrow(DuplicateSubwayTimetableException()).whenever(service).createTimetable("K450", payload)
        mockMvc
            .perform(
                post("/api/v1/subway/station/K450/timetable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isConflict)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("DUPLICATE_SUBWAY_TIMETABLE"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 항목 생성 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testCreateSubwayTimetableOtherException() {
        val payload =
            SubwayTimetableRequest(
                startStationID = "K410",
                terminalStationID = "K456",
                departureTime = "09:00:00",
                weekday = "weekdays",
                direction = "up",
            )
        doThrow(RuntimeException()).whenever(service).createTimetable("K450", payload)
        mockMvc
            .perform(
                post("/api/v1/subway/station/K450/timetable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 항목 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetSubwayTimetableById() {
        doReturn(
            SubwayTimetable(
                seq = 1,
                stationID = "K450",
                startStationID = "K410",
                terminalStationID = "K456",
                departureTime = LocalTime.parse("09:00"),
                weekday = "weekdays",
                heading = "up",
                station = null,
                startStation = null,
                terminalStation = null,
            ),
        ).whenever(service).getTimetableByStationIDAndSeq("K450", 1)
        mockMvc
            .perform(get("/api/v1/subway/station/K450/timetable/1"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.stationID").value("K450"))
            .andExpect(jsonPath("$.startStationID").value("K410"))
            .andExpect(jsonPath("$.terminalStationID").value("K456"))
            .andExpect(jsonPath("$.departureTime").value("09:00:00"))
            .andExpect(jsonPath("$.weekday").value("weekdays"))
            .andExpect(jsonPath("$.direction").value("up"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 항목 조회 - 없는 역 ID")
    @WithCustomMockUser(username = "test_user")
    fun testGetSubwayTimetableByIdStationNotFound() {
        doThrow(SubwayStationNotFoundException()).whenever(service).getTimetableByStationIDAndSeq("K499", 1)
        mockMvc
            .perform(get("/api/v1/subway/station/K499/timetable/1"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SUBWAY_STATION_NOT_FOUND"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 항목 조회 - 없는 시간표 seq")
    @WithCustomMockUser(username = "test_user")
    fun testGetSubwayTimetableByIdTimetableNotFound() {
        doThrow(SubwayTimetableNotFoundException()).whenever(service).getTimetableByStationIDAndSeq("K450", 99)
        mockMvc
            .perform(get("/api/v1/subway/station/K450/timetable/99"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SUBWAY_TIMETABLE_NOT_FOUND"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 항목 조회 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testGetSubwayTimetableByIdOtherException() {
        doThrow(RuntimeException()).whenever(service).getTimetableByStationIDAndSeq("K450", 1)
        mockMvc
            .perform(get("/api/v1/subway/station/K450/timetable/1"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 항목 수정")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateSubwayTimetable() {
        val updatedTimetable =
            SubwayTimetable(
                seq = 1,
                stationID = "K450",
                startStationID = "K410",
                terminalStationID = "K456",
                departureTime = LocalTime.parse("09:30"),
                weekday = "weekends",
                heading = "down",
                station = null,
                startStation = null,
                terminalStation = null,
            )
        val payload =
            mapOf(
                "startStationID" to "K410",
                "terminalStationID" to "K456",
                "departureTime" to "09:30:00",
                "weekday" to "weekends",
                "direction" to "down",
            )
        doReturn(updatedTimetable).whenever(service).updateTimetable(
            "K450",
            1,
            SubwayTimetableRequest(
                startStationID = "K410",
                terminalStationID = "K456",
                departureTime = "09:30:00",
                weekday = "weekends",
                direction = "down",
            ),
        )
        mockMvc
            .perform(
                put("/api/v1/subway/station/K450/timetable/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.stationID").value("K450"))
            .andExpect(jsonPath("$.startStationID").value("K410"))
            .andExpect(jsonPath("$.terminalStationID").value("K456"))
            .andExpect(jsonPath("$.departureTime").value("09:30:00"))
            .andExpect(jsonPath("$.weekday").value("weekends"))
            .andExpect(jsonPath("$.direction").value("down"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 항목 수정 - 없는 역 ID")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateSubwayTimetableStationNotFound() {
        val payload =
            SubwayTimetableRequest(
                startStationID = "K410",
                terminalStationID = "K456",
                departureTime = "09:30:00",
                weekday = "weekends",
                direction = "down",
            )
        doThrow(SubwayStationNotFoundException()).whenever(service).updateTimetable("K499", 1, payload)
        mockMvc
            .perform(
                put("/api/v1/subway/station/K499/timetable/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SUBWAY_STATION_NOT_FOUND"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 항목 수정 - 없는 시간표 seq")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateSubwayTimetableTimetableNotFound() {
        val payload =
            SubwayTimetableRequest(
                startStationID = "K410",
                terminalStationID = "K456",
                departureTime = "09:30:00",
                weekday = "weekends",
                direction = "down",
            )
        doThrow(SubwayTimetableNotFoundException()).whenever(service).updateTimetable("K450", 99, payload)
        mockMvc
            .perform(
                put("/api/v1/subway/station/K450/timetable/99")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SUBWAY_TIMETABLE_NOT_FOUND"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 항목 수정 - 존재하지 않는 출발역 ID")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateSubwayTimetableStartStationNotFound() {
        val payload =
            SubwayTimetableRequest(
                startStationID = "K499",
                terminalStationID = "K456",
                departureTime = "09:30:00",
                weekday = "weekends",
                direction = "down",
            )
        doThrow(SubwayStartStationNotFoundException()).whenever(service).updateTimetable("K450", 1, payload)
        mockMvc
            .perform(
                put("/api/v1/subway/station/K450/timetable/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SUBWAY_START_STATION_NOT_FOUND"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 항목 수정 - 존재하지 않는 종착역 ID")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateSubwayTimetableTerminalStationNotFound() {
        val payload =
            SubwayTimetableRequest(
                startStationID = "K410",
                terminalStationID = "K499",
                departureTime = "09:30:00",
                weekday = "weekends",
                direction = "down",
            )
        doThrow(SubwayTerminalStationNotFoundException()).whenever(service).updateTimetable("K450", 1, payload)
        mockMvc
            .perform(
                put("/api/v1/subway/station/K450/timetable/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SUBWAY_TERMINAL_STATION_NOT_FOUND"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 항목 수정 - 출발 시간 형식 오류")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateSubwayTimetableInvalidTimeFormat() {
        val payload =
            SubwayTimetableRequest(
                startStationID = "K410",
                terminalStationID = "K456",
                departureTime = "9시 반",
                weekday = "weekends",
                direction = "down",
            )
        doThrow(LocalTimeNotValidException()).whenever(service).updateTimetable("K450", 1, payload)
        mockMvc
            .perform(
                put("/api/v1/subway/station/K450/timetable/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INVALID_TIME_FORMAT"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 항목 수정 - 중복된 시간표")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateSubwayTimetableDuplicate() {
        val payload =
            SubwayTimetableRequest(
                startStationID = "K410",
                terminalStationID = "K456",
                departureTime = "09:30:00",
                weekday = "weekends",
                direction = "down",
            )
        doThrow(DuplicateSubwayTimetableException()).whenever(service).updateTimetable("K450", 1, payload)
        mockMvc
            .perform(
                put("/api/v1/subway/station/K450/timetable/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isConflict)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("DUPLICATE_SUBWAY_TIMETABLE"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 항목 수정 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateSubwayTimetableOtherException() {
        val payload =
            SubwayTimetableRequest(
                startStationID = "K410",
                terminalStationID = "K456",
                departureTime = "09:30:00",
                weekday = "weekends",
                direction = "down",
            )
        doThrow(RuntimeException()).whenever(service).updateTimetable("K450", 1, payload)
        mockMvc
            .perform(
                put("/api/v1/subway/station/K450/timetable/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 항목 삭제")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteSubwayTimetable() {
        mockMvc
            .perform(
                delete("/api/v1/subway/station/K450/timetable/1"),
            ).andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("지하철 역별 시간표 항목 삭제 - 없는 역 ID")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteSubwayTimetableStationNotFound() {
        doThrow(SubwayStationNotFoundException()).whenever(service).deleteTimetable("K499", 1)
        mockMvc
            .perform(
                delete("/api/v1/subway/station/K499/timetable/1"),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SUBWAY_STATION_NOT_FOUND"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 항목 삭제 - 없는 시간표 seq")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteSubwayTimetableTimetableNotFound() {
        doThrow(SubwayTimetableNotFoundException()).whenever(service).deleteTimetable("K450", 99)
        mockMvc
            .perform(
                delete("/api/v1/subway/station/K450/timetable/99"),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("SUBWAY_TIMETABLE_NOT_FOUND"))
    }

    @Test
    @DisplayName("지하철 역별 시간표 항목 삭제 - 기타 예외")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteSubwayTimetableOtherException() {
        doThrow(RuntimeException()).whenever(service).deleteTimetable("K450", 1)
        mockMvc
            .perform(
                delete("/api/v1/subway/station/K450/timetable/1"),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("전철 시간표 전체 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetAllSubwayTimetables() {
        doReturn(
            listOf(
                SubwayTimetable(
                    seq = 1,
                    stationID = "K450",
                    startStationID = "K410",
                    terminalStationID = "K456",
                    departureTime = LocalTime.parse("09:00"),
                    weekday = "weekdays",
                    heading = "up",
                    station = null,
                    startStation = null,
                    terminalStation = null,
                ),
                SubwayTimetable(
                    seq = 2,
                    stationID = "K451",
                    startStationID = "K410",
                    terminalStationID = "K456",
                    departureTime = LocalTime.parse("09:05"),
                    weekday = "weekdays",
                    heading = "up",
                    station = null,
                    startStation = null,
                    terminalStation = null,
                ),
            ),
        ).whenever(service).getAllTimetables()
        mockMvc
            .perform(get("/api/v1/subway/timetable"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.result").isArray)
            .andExpect(jsonPath("$.result[0].seq").value(1))
            .andExpect(jsonPath("$.result[0].stationID").value("K450"))
            .andExpect(jsonPath("$.result[0].startStationID").value("K410"))
            .andExpect(jsonPath("$.result[0].terminalStationID").value("K456"))
            .andExpect(jsonPath("$.result[0].departureTime").value("09:00:00"))
            .andExpect(jsonPath("$.result[0].weekday").value("weekdays"))
            .andExpect(jsonPath("$.result[0].direction").value("up"))
            .andExpect(jsonPath("$.result[1].seq").value(2))
            .andExpect(jsonPath("$.result[1].stationID").value("K451"))
            .andExpect(jsonPath("$.result[1].startStationID").value("K410"))
            .andExpect(jsonPath("$.result[1].terminalStationID").value("K456"))
            .andExpect(jsonPath("$.result[1].departureTime").value("09:05:00"))
            .andExpect(jsonPath("$.result[1].weekday").value("weekdays"))
            .andExpect(jsonPath("$.result[1].direction").value("up"))
    }

    @Test
    @DisplayName("전철 실시간 도착 정보 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetRealTimeArrivalInfo() {
        whenever(service.getRealtimeList()).thenReturn(
            listOf(
                SubwayRealtime(
                    stationID = "K450",
                    heading = "up",
                    order = 1,
                    location = "중앙",
                    remainingStop = 2,
                    remainingTime = Duration.ofMinutes(5),
                    terminalStationID = "K410",
                    trainNumber = "1234",
                    updatedAt = ZonedDateTime.now(),
                    isExpress = true,
                    isLast = false,
                    status = 99,
                    station = null,
                    terminalStation = null,
                ),
                SubwayRealtime(
                    stationID = "K251",
                    heading = "down",
                    order = 2,
                    location = "서울역",
                    remainingStop = 3,
                    remainingTime = Duration.ofMinutes(7),
                    terminalStationID = "K201",
                    trainNumber = "5678",
                    updatedAt = ZonedDateTime.now(),
                    isExpress = false,
                    isLast = true,
                    status = 100,
                    station = null,
                    terminalStation = null,
                ),
            ),
        )
        mockMvc
            .perform(get("/api/v1/subway/realtime"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.result").isArray)
            .andExpect(jsonPath("$.result[0].stationID").value("K450"))
            .andExpect(jsonPath("$.result[0].direction").value("up"))
            .andExpect(jsonPath("$.result[0].order").value(1))
            .andExpect(jsonPath("$.result[0].location").value("중앙"))
            .andExpect(jsonPath("$.result[0].stop").value(2))
            .andExpect(jsonPath("$.result[0].time").value("00:05:00"))
            .andExpect(jsonPath("$.result[0].terminalStationID").value("K410"))
            .andExpect(jsonPath("$.result[0].trainNumber").value("1234"))
            .andExpect(jsonPath("$.result[0].isExpress").value(true))
            .andExpect(jsonPath("$.result[0].isLast").value(false))
            .andExpect(jsonPath("$.result[0].status").value(99))
            .andExpect(jsonPath("$.result[1].stationID").value("K251"))
            .andExpect(jsonPath("$.result[1].direction").value("down"))
            .andExpect(jsonPath("$.result[1].order").value(2))
            .andExpect(jsonPath("$.result[1].location").value("서울역"))
            .andExpect(jsonPath("$.result[1].stop").value(3))
            .andExpect(jsonPath("$.result[1].time").value("00:07:00"))
            .andExpect(jsonPath("$.result[1].terminalStationID").value("K201"))
            .andExpect(jsonPath("$.result[1].trainNumber").value("5678"))
            .andExpect(jsonPath("$.result[1].isExpress").value(false))
            .andExpect(jsonPath("$.result[1].isLast").value(true))
            .andExpect(jsonPath("$.result[1].status").value(100))
    }
}
