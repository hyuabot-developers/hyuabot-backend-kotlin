package app.hyuabot.backend.shuttle

import app.hyuabot.backend.database.entity.ShuttleRoute
import app.hyuabot.backend.database.entity.ShuttleRouteStop
import app.hyuabot.backend.database.entity.ShuttleTimetable
import app.hyuabot.backend.database.exception.DurationNotValidException
import app.hyuabot.backend.security.WithCustomMockUser
import app.hyuabot.backend.shuttle.domain.CreateShuttleRouteRequest
import app.hyuabot.backend.shuttle.domain.CreateShuttleRouteStopRequest
import app.hyuabot.backend.shuttle.domain.ShuttleTimetableRequest
import app.hyuabot.backend.shuttle.domain.UpdateShuttleRouteRequest
import app.hyuabot.backend.shuttle.domain.UpdateShuttleRouteStopRequest
import app.hyuabot.backend.shuttle.exception.DuplicateShuttleRouteException
import app.hyuabot.backend.shuttle.exception.DuplicateShuttleRouteStopException
import app.hyuabot.backend.shuttle.exception.DuplicateShuttleTimetableException
import app.hyuabot.backend.shuttle.exception.ShuttleRouteNotFoundException
import app.hyuabot.backend.shuttle.exception.ShuttleRouteStopNotFoundException
import app.hyuabot.backend.shuttle.exception.ShuttleStopNotFoundException
import app.hyuabot.backend.shuttle.exception.ShuttleTimetableNotFoundException
import app.hyuabot.backend.shuttle.service.ShuttleRouteService
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShuttleRouteControllerTest {
    @MockitoBean
    private lateinit var shuttleRouteService: ShuttleRouteService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DisplayName("셔틀버스 노선 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun getShuttleRoutes() {
        doReturn(
            listOf(
                ShuttleRoute(
                    name = "A",
                    descriptionKorean = "A 노선",
                    descriptionEnglish = "Route A",
                    tag = "A",
                    startStopID = "Stop 1",
                    endStopID = "Stop 2",
                    timetable = emptyList(),
                    stop = emptyList(),
                    startStop = null,
                    endStop = null,
                ),
                ShuttleRoute(
                    name = "B",
                    descriptionKorean = "B 노선",
                    descriptionEnglish = "Route B",
                    tag = "B",
                    startStopID = "Stop 3",
                    endStopID = "Stop 4",
                    timetable = emptyList(),
                    stop = emptyList(),
                    startStop = null,
                    endStop = null,
                ),
            ),
        ).whenever(shuttleRouteService).getAllRoutes()

        mockMvc
            .perform(get("/api/v1/shuttle/route"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.result").isArray)
            .andExpect(jsonPath("$.result.length()").value(2))
            .andExpect(jsonPath("$.result[0].name").value("A"))
            .andExpect(jsonPath("$.result[0].descriptionKorean").value("A 노선"))
            .andExpect(jsonPath("$.result[0].descriptionEnglish").value("Route A"))
            .andExpect(jsonPath("$.result[0].tag").value("A"))
            .andExpect(jsonPath("$.result[0].startStopID").value("Stop 1"))
            .andExpect(jsonPath("$.result[0].endStopID").value("Stop 2"))
            .andExpect(jsonPath("$.result[1].name").value("B"))
            .andExpect(jsonPath("$.result[1].descriptionKorean").value("B 노선"))
            .andExpect(jsonPath("$.result[1].descriptionEnglish").value("Route B"))
            .andExpect(jsonPath("$.result[1].tag").value("B"))
            .andExpect(jsonPath("$.result[1].startStopID").value("Stop 3"))
            .andExpect(jsonPath("$.result[1].endStopID").value("Stop 4"))
    }

    @Test
    @DisplayName("셔틀버스 노선 생성")
    @WithCustomMockUser(username = "test_user")
    fun createShuttleRoute() {
        val newRoute =
            ShuttleRoute(
                name = "C",
                descriptionKorean = "C 노선",
                descriptionEnglish = "Route C",
                tag = "C",
                startStopID = "Stop 5",
                endStopID = "Stop 6",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        val payload =
            CreateShuttleRouteRequest(
                name = "C",
                descriptionKorean = "C 노선",
                descriptionEnglish = "Route C",
                tag = "C",
                startStopID = "Stop 5",
                endStopID = "Stop 6",
            )

        doReturn(newRoute).whenever(shuttleRouteService).createRoute(payload)

        mockMvc
            .perform(
                post("/api/v1/shuttle/route")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isCreated)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.name").value("C"))
            .andExpect(jsonPath("$.descriptionKorean").value("C 노선"))
            .andExpect(jsonPath("$.descriptionEnglish").value("Route C"))
            .andExpect(jsonPath("$.tag").value("C"))
            .andExpect(jsonPath("$.startStopID").value("Stop 5"))
            .andExpect(jsonPath("$.endStopID").value("Stop 6"))
    }

    @Test
    @DisplayName("셔틀버스 노선 생성 (중복된 이름)")
    @WithCustomMockUser(username = "test_user")
    fun createShuttleRouteDuplicateName() {
        val payload =
            CreateShuttleRouteRequest(
                name = "A",
                descriptionKorean = "A 노선",
                descriptionEnglish = "Route A",
                tag = "A",
                startStopID = "Stop 1",
                endStopID = "Stop 2",
            )

        doThrow(DuplicateShuttleRouteException::class).whenever(shuttleRouteService).createRoute(payload)

        mockMvc
            .perform(
                post("/api/v1/shuttle/route")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isConflict)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("DUPLICATE_SHUTTLE_ROUTE"))
    }

    @Test
    @DisplayName("셔틀버스 노선 생성 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun createShuttleRouteOtherError() {
        val payload =
            CreateShuttleRouteRequest(
                name = "D",
                descriptionKorean = "D 노선",
                descriptionEnglish = "Route D",
                tag = "D",
                startStopID = "Stop 7",
                endStopID = "Stop 8",
            )

        doThrow(RuntimeException("Unexpected error")).whenever(shuttleRouteService).createRoute(payload)

        mockMvc
            .perform(
                post("/api/v1/shuttle/route")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("셔틀버스 노선 상세 조회")
    @WithCustomMockUser(username = "test_user")
    fun getShuttleRouteByName() {
        val routeName = "A"
        doReturn(
            ShuttleRoute(
                name = "A",
                descriptionKorean = "A 노선",
                descriptionEnglish = "Route A",
                tag = "A",
                startStopID = "Stop 1",
                endStopID = "Stop 2",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            ),
        ).whenever(shuttleRouteService).getRouteByName(routeName)

        mockMvc
            .perform(get("/api/v1/shuttle/route/$routeName"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.name").value("A"))
            .andExpect(jsonPath("$.descriptionKorean").value("A 노선"))
            .andExpect(jsonPath("$.descriptionEnglish").value("Route A"))
            .andExpect(jsonPath("$.tag").value("A"))
            .andExpect(jsonPath("$.startStopID").value("Stop 1"))
            .andExpect(jsonPath("$.endStopID").value("Stop 2"))
    }

    @Test
    @DisplayName("셔틀버스 노선 상세 조회 (존재하지 않는 노선)")
    @WithCustomMockUser(username = "test_user")
    fun getShuttleRouteByNameNotFound() {
        val routeName = "NonExistentRoute"
        doThrow(ShuttleRouteNotFoundException::class).whenever(shuttleRouteService).getRouteByName(routeName)

        mockMvc
            .perform(get("/api/v1/shuttle/route/$routeName"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 노선 상세 조회 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun getShuttleRouteByNameOtherError() {
        val routeName = "A"
        doThrow(RuntimeException("Unexpected error")).whenever(shuttleRouteService).getRouteByName(routeName)

        mockMvc
            .perform(get("/api/v1/shuttle/route/$routeName"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("셔틀버스 노선 수정")
    @WithCustomMockUser(username = "test_user")
    fun updateShuttleRoute() {
        val updatedRoute =
            ShuttleRoute(
                name = "A",
                descriptionKorean = "Updated A 노선",
                descriptionEnglish = "Updated Route A",
                tag = "A",
                startStopID = "Stop 1",
                endStopID = "Stop 2",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        val payload =
            UpdateShuttleRouteRequest(
                descriptionKorean = "Updated A 노선",
                descriptionEnglish = "Updated Route A",
                tag = "A",
                startStopID = "Stop 1",
                endStopID = "Stop 2",
            )

        doReturn(updatedRoute).whenever(shuttleRouteService).updateRoute("A", payload)

        mockMvc
            .perform(
                put("/api/v1/shuttle/route/A")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.name").value("A"))
            .andExpect(jsonPath("$.descriptionKorean").value("Updated A 노선"))
            .andExpect(jsonPath("$.descriptionEnglish").value("Updated Route A"))
            .andExpect(jsonPath("$.tag").value("A"))
            .andExpect(jsonPath("$.startStopID").value("Stop 1"))
            .andExpect(jsonPath("$.endStopID").value("Stop 2"))
    }

    @Test
    @DisplayName("셔틀버스 노선 수정 (존재하지 않는 노선)")
    @WithCustomMockUser(username = "test_user")
    fun updateShuttleRouteNotFound() {
        val payload =
            UpdateShuttleRouteRequest(
                descriptionKorean = "Updated A 노선",
                descriptionEnglish = "Updated Route A",
                tag = "A",
                startStopID = "Stop 1",
                endStopID = "Stop 2",
            )

        doThrow(ShuttleRouteNotFoundException::class).whenever(shuttleRouteService).updateRoute("NonExistentRoute", payload)

        mockMvc
            .perform(
                put("/api/v1/shuttle/route/NonExistentRoute")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 노선 수정 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun updateShuttleRouteOtherError() {
        val payload =
            UpdateShuttleRouteRequest(
                descriptionKorean = "Updated A 노선",
                descriptionEnglish = "Updated Route A",
                tag = "A",
                startStopID = "Stop 1",
                endStopID = "Stop 2",
            )

        doThrow(RuntimeException("Unexpected error")).whenever(shuttleRouteService).updateRoute("A", payload)

        mockMvc
            .perform(
                put("/api/v1/shuttle/route/A")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("셔틀버스 노선 삭제")
    @WithCustomMockUser(username = "test_user")
    fun deleteShuttleRoute() {
        val routeName = "A"
        doNothing().whenever(shuttleRouteService).deleteRouteByName(routeName)

        mockMvc
            .perform(delete("/api/v1/shuttle/route/$routeName"))
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("셔틀버스 노선 삭제 (존재하지 않는 노선)")
    @WithCustomMockUser(username = "test_user")
    fun deleteShuttleRouteNotFound() {
        val routeName = "NonExistentRoute"
        doThrow(ShuttleRouteNotFoundException::class).whenever(shuttleRouteService).deleteRouteByName(routeName)

        mockMvc
            .perform(delete("/api/v1/shuttle/route/$routeName"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 노선 삭제 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun deleteShuttleRouteOtherError() {
        val routeName = "A"
        doThrow(RuntimeException("Unexpected error")).whenever(shuttleRouteService).deleteRouteByName(routeName)

        mockMvc
            .perform(delete("/api/v1/shuttle/route/$routeName"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun getShuttleRouteStops() {
        val routeName = "A"
        doReturn(
            listOf(
                ShuttleRouteStop(
                    seq = 1,
                    stopName = "Stop1",
                    routeName = routeName,
                    order = 1,
                    cumulativeTime = Duration.ofMinutes(5),
                    route = null,
                    stop = null,
                ),
                ShuttleRouteStop(
                    seq = 2,
                    stopName = "Stop2",
                    routeName = routeName,
                    order = 2,
                    cumulativeTime = Duration.ofMinutes(10),
                    route = null,
                    stop = null,
                ),
                ShuttleRouteStop(
                    seq = 3,
                    stopName = "Stop3",
                    routeName = routeName,
                    order = 3,
                    cumulativeTime = Duration.ofMinutes(-10),
                    route = null,
                    stop = null,
                ),
            ),
        ).whenever(shuttleRouteService).getShuttleStopByRouteName(routeName)

        mockMvc
            .perform(get("/api/v1/shuttle/route/$routeName/stop"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.result").isArray)
            .andExpect(jsonPath("$.result.length()").value(3))
            .andExpect(jsonPath("$.result[0].name").value("Stop1"))
            .andExpect(jsonPath("$.result[0].order").value(1))
            .andExpect(jsonPath("$.result[0].cumulativeTime").value("00:05:00"))
            .andExpect(jsonPath("$.result[1].name").value("Stop2"))
            .andExpect(jsonPath("$.result[1].order").value(2))
            .andExpect(jsonPath("$.result[1].cumulativeTime").value("00:10:00"))
            .andExpect(jsonPath("$.result[2].name").value("Stop3"))
            .andExpect(jsonPath("$.result[2].order").value(3))
            .andExpect(jsonPath("$.result[2].cumulativeTime").value("-00:10:00"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 목록 조회 (존재하지 않는 노선)")
    @WithCustomMockUser(username = "test_user")
    fun getShuttleRouteStopsNotFound() {
        val routeName = "NonExistentRoute"
        doThrow(ShuttleRouteNotFoundException::class).whenever(shuttleRouteService).getShuttleStopByRouteName(routeName)

        mockMvc
            .perform(get("/api/v1/shuttle/route/$routeName/stop"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 목록 조회 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun getShuttleRouteStopsOtherError() {
        val routeName = "A"
        doThrow(RuntimeException("Unexpected error")).whenever(shuttleRouteService).getShuttleStopByRouteName(routeName)

        mockMvc
            .perform(get("/api/v1/shuttle/route/$routeName/stop"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 추가")
    @WithCustomMockUser(username = "test_user")
    fun addShuttleRouteStop() {
        val routeName = "A"
        val newStop =
            ShuttleRouteStop(
                seq = 3,
                stopName = "Stop3",
                routeName = routeName,
                order = 3,
                cumulativeTime = Duration.ofMinutes(15),
                route = null,
                stop = null,
            )
        val payload =
            CreateShuttleRouteStopRequest(
                stopName = "Stop3",
                order = 3,
                cumulativeTime = "00:15:00",
            )

        doReturn(newStop).whenever(shuttleRouteService).createShuttleRouteStop(routeName, payload)

        mockMvc
            .perform(
                post("/api/v1/shuttle/route/$routeName/stop")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isCreated)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.name").value("Stop3"))
            .andExpect(jsonPath("$.order").value(3))
            .andExpect(jsonPath("$.cumulativeTime").value("00:15:00"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 추가 (존재하지 않는 노선)")
    @WithCustomMockUser(username = "test_user")
    fun addShuttleRouteStopNotFound() {
        val routeName = "NonExistentRoute"
        val payload =
            CreateShuttleRouteStopRequest(
                stopName = "Stop3",
                order = 3,
                cumulativeTime = "00:15:00",
            )

        doThrow(ShuttleRouteNotFoundException::class).whenever(shuttleRouteService).createShuttleRouteStop(routeName, payload)

        mockMvc
            .perform(
                post("/api/v1/shuttle/route/$routeName/stop")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 추가 (존재하지 않는 정류장)")
    @WithCustomMockUser(username = "test_user")
    fun addShuttleRouteStopInvalidStop() {
        val routeName = "A"
        val payload =
            CreateShuttleRouteStopRequest(
                stopName = "NonExistentStop",
                order = 3,
                cumulativeTime = "00:15:00",
            )

        doThrow(ShuttleStopNotFoundException::class).whenever(shuttleRouteService).createShuttleRouteStop(routeName, payload)

        mockMvc
            .perform(
                post("/api/v1/shuttle/route/$routeName/stop")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("SHUTTLE_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 추가 (중복된 정류장)")
    @WithCustomMockUser(username = "test_user")
    fun addShuttleRouteStopDuplicate() {
        val routeName = "A"
        val payload =
            CreateShuttleRouteStopRequest(
                stopName = "Stop1",
                order = 1,
                cumulativeTime = "00:05:00",
            )

        doThrow(DuplicateShuttleRouteStopException::class).whenever(shuttleRouteService).createShuttleRouteStop(routeName, payload)

        mockMvc
            .perform(
                post("/api/v1/shuttle/route/$routeName/stop")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isConflict)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("DUPLICATE_SHUTTLE_ROUTE_STOP"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 추가 (잘못된 시간 형식)")
    @WithCustomMockUser(username = "test_user")
    fun addShuttleRouteStopInvalidTimeFormat() {
        val routeName = "A"
        val payload =
            CreateShuttleRouteStopRequest(
                stopName = "Stop3",
                order = 3,
                cumulativeTime = "invalid_time_format",
            )

        doThrow(DurationNotValidException::class).whenever(shuttleRouteService).createShuttleRouteStop(routeName, payload)

        mockMvc
            .perform(
                post("/api/v1/shuttle/route/$routeName/stop")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INVALID_DATE_TIME_FORMAT"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 추가 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun addShuttleRouteStopOtherError() {
        val routeName = "A"
        val payload =
            CreateShuttleRouteStopRequest(
                stopName = "Stop3",
                order = 3,
                cumulativeTime = "00:15:00",
            )

        doThrow(RuntimeException("Unexpected error")).whenever(shuttleRouteService).createShuttleRouteStop(routeName, payload)

        mockMvc
            .perform(
                post("/api/v1/shuttle/route/$routeName/stop")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 상세 조회")
    @WithCustomMockUser(username = "test_user")
    fun getShuttleRouteStopByName() {
        val routeName = "A"
        val stopName = "Stop1"
        doReturn(
            ShuttleRouteStop(
                seq = 1,
                stopName = stopName,
                routeName = routeName,
                order = 1,
                cumulativeTime = Duration.ofMinutes(5),
                route = null,
                stop = null,
            ),
        ).whenever(shuttleRouteService).getShuttleRouteStopByRouteNameAndStopName(routeName, stopName)

        mockMvc
            .perform(get("/api/v1/shuttle/route/$routeName/stop/$stopName"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.name").value("Stop1"))
            .andExpect(jsonPath("$.order").value(1))
            .andExpect(jsonPath("$.cumulativeTime").value("00:05:00"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 상세 조회 (존재하지 않는 노선)")
    @WithCustomMockUser(username = "test_user")
    fun getShuttleRouteStopByNameNotFoundRoute() {
        val routeName = "NonExistentRoute"
        val stopName = "Stop1"
        doThrow(
            ShuttleRouteNotFoundException::class,
        ).whenever(shuttleRouteService).getShuttleRouteStopByRouteNameAndStopName(routeName, stopName)

        mockMvc
            .perform(get("/api/v1/shuttle/route/$routeName/stop/$stopName"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 상세 조회 (존재하지 않는 정류장)")
    @WithCustomMockUser(username = "test_user")
    fun getShuttleRouteStopByNameNotFoundStop() {
        val routeName = "A"
        val stopName = "NonExistentStop"
        doThrow(
            ShuttleRouteStopNotFoundException::class,
        ).whenever(shuttleRouteService).getShuttleRouteStopByRouteNameAndStopName(routeName, stopName)

        mockMvc
            .perform(get("/api/v1/shuttle/route/$routeName/stop/$stopName"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 상세 조회 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun getShuttleRouteStopByNameOtherError() {
        val routeName = "A"
        val stopName = "Stop1"
        doThrow(
            RuntimeException("Unexpected error"),
        ).whenever(shuttleRouteService).getShuttleRouteStopByRouteNameAndStopName(routeName, stopName)

        mockMvc
            .perform(get("/api/v1/shuttle/route/$routeName/stop/$stopName"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 수정")
    @WithCustomMockUser(username = "test_user")
    fun updateShuttleRouteStop() {
        val routeName = "A"
        val stopName = "Stop1"
        val updatedStop =
            ShuttleRouteStop(
                seq = 1,
                stopName = stopName,
                routeName = routeName,
                order = 1,
                cumulativeTime = Duration.ofMinutes(10),
                route = null,
                stop = null,
            )
        val payload =
            UpdateShuttleRouteStopRequest(
                order = 1,
                cumulativeTime = "00:10:00",
            )

        doReturn(updatedStop).whenever(shuttleRouteService).updateShuttleRouteStop(routeName, stopName, payload)

        mockMvc
            .perform(
                put("/api/v1/shuttle/route/$routeName/stop/$stopName")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.seq").value(1))
            .andExpect(jsonPath("$.name").value("Stop1"))
            .andExpect(jsonPath("$.order").value(1))
            .andExpect(jsonPath("$.cumulativeTime").value("00:10:00"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 수정 (존재하지 않는 노선)")
    @WithCustomMockUser(username = "test_user")
    fun updateShuttleRouteStopNotFoundRoute() {
        val routeName = "NonExistentRoute"
        val stopName = "Stop1"
        val payload =
            UpdateShuttleRouteStopRequest(
                order = 1,
                cumulativeTime = "00:10:00",
            )

        doThrow(ShuttleRouteNotFoundException::class).whenever(shuttleRouteService).updateShuttleRouteStop(routeName, stopName, payload)

        mockMvc
            .perform(
                put("/api/v1/shuttle/route/$routeName/stop/$stopName")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 수정 (존재하지 않는 정류장)")
    @WithCustomMockUser(username = "test_user")
    fun updateShuttleRouteStopNotFoundStop() {
        val routeName = "A"
        val stopName = "NonExistentStop"
        val payload =
            UpdateShuttleRouteStopRequest(
                order = 1,
                cumulativeTime = "00:10:00",
            )

        doThrow(ShuttleRouteStopNotFoundException::class).whenever(shuttleRouteService).updateShuttleRouteStop(routeName, stopName, payload)

        mockMvc
            .perform(
                put("/api/v1/shuttle/route/$routeName/stop/$stopName")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 수정 (잘못된 시간 형식)")
    @WithCustomMockUser(username = "test_user")
    fun updateShuttleRouteStopInvalidTimeFormat() {
        val routeName = "A"
        val stopName = "Stop1"
        val payload =
            UpdateShuttleRouteStopRequest(
                order = 1,
                cumulativeTime = "invalid_time_format",
            )
        doThrow(DurationNotValidException::class)
            .whenever(shuttleRouteService)
            .updateShuttleRouteStop(routeName, stopName, payload)

        mockMvc
            .perform(
                put("/api/v1/shuttle/route/$routeName/stop/$stopName")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INVALID_DATE_TIME_FORMAT"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 수정 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun updateShuttleRouteStopOtherError() {
        val routeName = "A"
        val stopName = "Stop1"
        val payload =
            UpdateShuttleRouteStopRequest(
                order = 1,
                cumulativeTime = "00:10:00",
            )

        doThrow(RuntimeException("Unexpected error")).whenever(shuttleRouteService).updateShuttleRouteStop(routeName, stopName, payload)

        mockMvc
            .perform(
                put("/api/v1/shuttle/route/$routeName/stop/$stopName")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 삭제")
    @WithCustomMockUser(username = "test_user")
    fun deleteShuttleRouteStop() {
        val routeName = "A"
        val stopName = "Stop1"
        doNothing().whenever(shuttleRouteService).deleteShuttleRouteStop(routeName, stopName)

        mockMvc
            .perform(delete("/api/v1/shuttle/route/$routeName/stop/$stopName"))
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 삭제 (존재하지 않는 노선)")
    @WithCustomMockUser(username = "test_user")
    fun deleteShuttleRouteStopNotFoundRoute() {
        val routeName = "NonExistentRoute"
        val stopName = "Stop1"
        doThrow(ShuttleRouteNotFoundException::class).whenever(shuttleRouteService).deleteShuttleRouteStop(routeName, stopName)

        mockMvc
            .perform(delete("/api/v1/shuttle/route/$routeName/stop/$stopName"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 삭제 (존재하지 않는 정류장)")
    @WithCustomMockUser(username = "test_user")
    fun deleteShuttleRouteStopNotFoundStop() {
        val routeName = "A"
        val stopName = "NonExistentStop"
        doThrow(ShuttleRouteStopNotFoundException::class).whenever(shuttleRouteService).deleteShuttleRouteStop(routeName, stopName)

        mockMvc
            .perform(delete("/api/v1/shuttle/route/$routeName/stop/$stopName"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 삭제 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun deleteShuttleRouteStopOtherError() {
        val routeName = "A"
        val stopName = "Stop1"
        doThrow(RuntimeException("Unexpected error")).whenever(shuttleRouteService).deleteShuttleRouteStop(routeName, stopName)

        mockMvc
            .perform(delete("/api/v1/shuttle/route/$routeName/stop/$stopName"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun getShuttleRouteTimetable() {
        val routeName = "A"
        doReturn(
            listOf(
                ShuttleTimetable(
                    seq = 1,
                    periodType = "semester",
                    weekday = true,
                    routeName = routeName,
                    departureTime = LocalTime.of(12, 0),
                    route = null,
                ),
                ShuttleTimetable(
                    seq = 2,
                    periodType = "vacation",
                    weekday = false,
                    routeName = routeName,
                    departureTime = LocalTime.of(13, 0),
                    route = null,
                ),
            ),
        ).whenever(shuttleRouteService).getShuttleTimetableByRouteName(routeName, null, null)

        mockMvc
            .perform(get("/api/v1/shuttle/route/$routeName/timetable"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.result").isArray)
            .andExpect(jsonPath("$.result.length()").value(2))
            .andExpect(jsonPath("$.result[0].period").value("semester"))
            .andExpect(jsonPath("$.result[0].isWeekdays").value(true))
            .andExpect(jsonPath("$.result[0].departureTime").value("12:00:00"))
            .andExpect(jsonPath("$.result[1].period").value("vacation"))
            .andExpect(jsonPath("$.result[1].isWeekdays").value(false))
            .andExpect(jsonPath("$.result[1].departureTime").value("13:00:00"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 목록 조회 (존재하지 않는 노선)")
    @WithCustomMockUser(username = "test_user")
    fun getShuttleRouteTimetableNotFound() {
        val routeName = "NonExistentRoute"
        doThrow(ShuttleRouteNotFoundException::class).whenever(shuttleRouteService).getShuttleTimetableByRouteName(routeName, null, null)

        mockMvc
            .perform(get("/api/v1/shuttle/route/$routeName/timetable"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 목록 조회 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun getShuttleRouteTimetableOtherError() {
        val routeName = "A"
        doThrow(RuntimeException("Unexpected error")).whenever(shuttleRouteService).getShuttleTimetableByRouteName(routeName, null, null)

        mockMvc
            .perform(get("/api/v1/shuttle/route/$routeName/timetable"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 추가")
    @WithCustomMockUser(username = "test_user")
    fun addShuttleRouteTimetable() {
        val routeName = "A"
        val newTimetable =
            ShuttleTimetable(
                seq = 3,
                periodType = "semester",
                weekday = true,
                routeName = routeName,
                departureTime = LocalTime.of(14, 0),
                route = null,
            )
        val payload =
            ShuttleTimetableRequest(
                periodType = "semester",
                weekday = true,
                departureTime = "14:00:00",
            )

        doReturn(newTimetable).whenever(shuttleRouteService).createShuttleTimetable(routeName, payload)

        mockMvc
            .perform(
                post("/api/v1/shuttle/route/$routeName/timetable")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isCreated)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.period").value("semester"))
            .andExpect(jsonPath("$.isWeekdays").value(true))
            .andExpect(jsonPath("$.departureTime").value("14:00:00"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 추가 (존재하지 않는 노선)")
    @WithCustomMockUser(username = "test_user")
    fun addShuttleRouteTimetableNotFound() {
        val routeName = "NonExistentRoute"
        val payload =
            ShuttleTimetableRequest(
                periodType = "semester",
                weekday = true,
                departureTime = "14:00:00",
            )

        doThrow(ShuttleRouteNotFoundException::class).whenever(shuttleRouteService).createShuttleTimetable(routeName, payload)

        mockMvc
            .perform(
                post("/api/v1/shuttle/route/$routeName/timetable")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 추가 (잘못된 시간 형식)")
    @WithCustomMockUser(username = "test_user")
    fun addShuttleRouteTimetableInvalidTimeFormat() {
        val routeName = "A"
        val payload =
            ShuttleTimetableRequest(
                periodType = "semester",
                weekday = true,
                departureTime = "invalid_time_format",
            )

        doThrow(DurationNotValidException::class).whenever(shuttleRouteService).createShuttleTimetable(routeName, payload)

        mockMvc
            .perform(
                post("/api/v1/shuttle/route/$routeName/timetable")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INVALID_DATE_TIME_FORMAT"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 추가 (중복된 시간표)")
    @WithCustomMockUser(username = "test_user")
    fun addShuttleRouteTimetableDuplicate() {
        val routeName = "A"
        val payload =
            ShuttleTimetableRequest(
                periodType = "semester",
                weekday = true,
                departureTime = "12:00:00",
            )

        doThrow(DuplicateShuttleTimetableException::class).whenever(shuttleRouteService).createShuttleTimetable(routeName, payload)

        mockMvc
            .perform(
                post("/api/v1/shuttle/route/$routeName/timetable")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isConflict)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("DUPLICATE_SHUTTLE_TIMETABLE"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 추가 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun addShuttleRouteTimetableOtherError() {
        val routeName = "A"
        val payload =
            ShuttleTimetableRequest(
                periodType = "semester",
                weekday = true,
                departureTime = "14:00:00",
            )

        doThrow(RuntimeException("Unexpected error")).whenever(shuttleRouteService).createShuttleTimetable(routeName, payload)

        mockMvc
            .perform(
                post("/api/v1/shuttle/route/$routeName/timetable")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 상세 조회")
    @WithCustomMockUser(username = "test_user")
    fun getShuttleRouteTimetableBySeq() {
        val routeName = "A"
        val timetableSeq = 1
        doReturn(
            ShuttleTimetable(
                seq = timetableSeq,
                periodType = "semester",
                weekday = true,
                routeName = routeName,
                departureTime = LocalTime.of(12, 0),
                route = null,
            ),
        ).whenever(shuttleRouteService).getShuttleTimetableByRouteNameAndSeq(routeName, timetableSeq)

        mockMvc
            .perform(get("/api/v1/shuttle/route/$routeName/timetable/$timetableSeq"))
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.seq").value(timetableSeq))
            .andExpect(jsonPath("$.period").value("semester"))
            .andExpect(jsonPath("$.isWeekdays").value(true))
            .andExpect(jsonPath("$.departureTime").value("12:00:00"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 상세 조회 (존재하지 않는 노선)")
    @WithCustomMockUser(username = "test_user")
    fun getShuttleRouteTimetableBySeqNotFoundRoute() {
        val routeName = "NonExistentRoute"
        val timetableSeq = 1
        doThrow(
            ShuttleRouteNotFoundException::class,
        ).whenever(shuttleRouteService).getShuttleTimetableByRouteNameAndSeq(routeName, timetableSeq)

        mockMvc
            .perform(get("/api/v1/shuttle/route/$routeName/timetable/$timetableSeq"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 상세 조회 (존재하지 않는 시간표)")
    @WithCustomMockUser(username = "test_user")
    fun getShuttleRouteTimetableBySeqNotFoundTimetable() {
        val routeName = "A"
        val timetableSeq = 999
        doThrow(
            ShuttleTimetableNotFoundException::class,
        ).whenever(shuttleRouteService).getShuttleTimetableByRouteNameAndSeq(routeName, timetableSeq)

        mockMvc
            .perform(get("/api/v1/shuttle/route/$routeName/timetable/$timetableSeq"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("SHUTTLE_TIMETABLE_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 상세 조회 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun getShuttleRouteTimetableBySeqOtherError() {
        val routeName = "A"
        val timetableSeq = 1
        doThrow(
            RuntimeException("Unexpected error"),
        ).whenever(shuttleRouteService).getShuttleTimetableByRouteNameAndSeq(routeName, timetableSeq)

        mockMvc
            .perform(get("/api/v1/shuttle/route/$routeName/timetable/$timetableSeq"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 수정")
    @WithCustomMockUser(username = "test_user")
    fun updateShuttleRouteTimetable() {
        val routeName = "A"
        val timetableSeq = 1
        val updatedTimetable =
            ShuttleTimetable(
                seq = timetableSeq,
                periodType = "semester",
                weekday = true,
                routeName = routeName,
                departureTime = LocalTime.of(15, 0),
                route = null,
            )
        val payload =
            ShuttleTimetableRequest(
                periodType = "semester",
                weekday = true,
                departureTime = "15:00:00",
            )

        doReturn(updatedTimetable).whenever(shuttleRouteService).updateShuttleTimetable(routeName, timetableSeq, payload)

        mockMvc
            .perform(
                put("/api/v1/shuttle/route/$routeName/timetable/$timetableSeq")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.period").value("semester"))
            .andExpect(jsonPath("$.isWeekdays").value(true))
            .andExpect(jsonPath("$.departureTime").value("15:00:00"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 수정 (존재하지 않는 노선)")
    @WithCustomMockUser(username = "test_user")
    fun updateShuttleRouteTimetableNotFoundRoute() {
        val routeName = "NonExistentRoute"
        val timetableSeq = 1
        val payload =
            ShuttleTimetableRequest(
                periodType = "semester",
                weekday = true,
                departureTime = "15:00:00",
            )

        doThrow(ShuttleRouteNotFoundException::class).whenever(shuttleRouteService).updateShuttleTimetable(routeName, timetableSeq, payload)

        mockMvc
            .perform(
                put("/api/v1/shuttle/route/$routeName/timetable/$timetableSeq")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 수정 (존재하지 않는 시간표)")
    @WithCustomMockUser(username = "test_user")
    fun updateShuttleRouteTimetableNotFoundTimetable() {
        val routeName = "A"
        val timetableSeq = 999
        val payload =
            ShuttleTimetableRequest(
                periodType = "semester",
                weekday = true,
                departureTime = "15:00:00",
            )

        doThrow(
            ShuttleTimetableNotFoundException::class,
        ).whenever(shuttleRouteService).updateShuttleTimetable(routeName, timetableSeq, payload)

        mockMvc
            .perform(
                put("/api/v1/shuttle/route/$routeName/timetable/$timetableSeq")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("SHUTTLE_TIMETABLE_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 수정 (잘못된 시간 형식)")
    @WithCustomMockUser(username = "test_user")
    fun updateShuttleRouteTimetableInvalidTimeFormat() {
        val routeName = "A"
        val timetableSeq = 1
        val payload =
            ShuttleTimetableRequest(
                periodType = "semester",
                weekday = true,
                departureTime = "invalid_time_format",
            )

        doThrow(DurationNotValidException::class).whenever(shuttleRouteService).updateShuttleTimetable(routeName, timetableSeq, payload)

        mockMvc
            .perform(
                put("/api/v1/shuttle/route/$routeName/timetable/$timetableSeq")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INVALID_DATE_TIME_FORMAT"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 수정 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun updateShuttleRouteTimetableOtherError() {
        val routeName = "A"
        val timetableSeq = 1
        val payload =
            ShuttleTimetableRequest(
                periodType = "semester",
                weekday = true,
                departureTime = "15:00:00",
            )

        doThrow(RuntimeException("Unexpected error")).whenever(shuttleRouteService).updateShuttleTimetable(routeName, timetableSeq, payload)

        mockMvc
            .perform(
                put("/api/v1/shuttle/route/$routeName/timetable/$timetableSeq")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isInternalServerError)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 삭제")
    @WithCustomMockUser(username = "test_user")
    fun deleteShuttleRouteTimetable() {
        val routeName = "A"
        val timetableSeq = 1
        doNothing().whenever(shuttleRouteService).deleteShuttleTimetable(routeName, timetableSeq)

        mockMvc
            .perform(delete("/api/v1/shuttle/route/$routeName/timetable/$timetableSeq"))
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 삭제 (존재하지 않는 노선)")
    @WithCustomMockUser(username = "test_user")
    fun deleteShuttleRouteTimetableNotFoundRoute() {
        val routeName = "NonExistentRoute"
        val timetableSeq = 1
        doThrow(ShuttleRouteNotFoundException::class).whenever(shuttleRouteService).deleteShuttleTimetable(routeName, timetableSeq)

        mockMvc
            .perform(delete("/api/v1/shuttle/route/$routeName/timetable/$timetableSeq"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("SHUTTLE_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 삭제 (존재하지 않는 시간표)")
    @WithCustomMockUser(username = "test_user")
    fun deleteShuttleRouteTimetableNotFoundTimetable() {
        val routeName = "A"
        val timetableSeq = 999
        doThrow(ShuttleTimetableNotFoundException::class).whenever(shuttleRouteService).deleteShuttleTimetable(routeName, timetableSeq)

        mockMvc
            .perform(delete("/api/v1/shuttle/route/$routeName/timetable/$timetableSeq"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("SHUTTLE_TIMETABLE_NOT_FOUND"))
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 삭제 (기타 오류)")
    @WithCustomMockUser(username = "test_user")
    fun deleteShuttleRouteTimetableOtherError() {
        val routeName = "A"
        val timetableSeq = 1
        doThrow(RuntimeException("Unexpected error")).whenever(shuttleRouteService).deleteShuttleTimetable(routeName, timetableSeq)

        mockMvc
            .perform(delete("/api/v1/shuttle/route/$routeName/timetable/$timetableSeq"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }
}
