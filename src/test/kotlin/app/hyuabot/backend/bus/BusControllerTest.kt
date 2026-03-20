package app.hyuabot.backend.bus

import app.hyuabot.backend.bus.domain.BusRouteStopRequest
import app.hyuabot.backend.bus.domain.BusTimetableRequest
import app.hyuabot.backend.bus.domain.CreateBusRouteRequest
import app.hyuabot.backend.bus.domain.CreateBusStopRequest
import app.hyuabot.backend.bus.domain.UpdateBusRouteRequest
import app.hyuabot.backend.bus.domain.UpdateBusStopRequest
import app.hyuabot.backend.bus.exception.BusEndStopNotFoundException
import app.hyuabot.backend.bus.exception.BusRouteNotFoundException
import app.hyuabot.backend.bus.exception.BusRouteStopNotFoundException
import app.hyuabot.backend.bus.exception.BusStartStopNotFoundException
import app.hyuabot.backend.bus.exception.BusStopNotFoundException
import app.hyuabot.backend.bus.exception.BusTimetableNotFoundException
import app.hyuabot.backend.bus.exception.DuplicateBusRouteException
import app.hyuabot.backend.bus.exception.DuplicateBusRouteStopException
import app.hyuabot.backend.bus.exception.DuplicateBusStopException
import app.hyuabot.backend.bus.exception.DuplicateBusTimetableException
import app.hyuabot.backend.bus.service.BusRealtimeService
import app.hyuabot.backend.bus.service.BusRouteService
import app.hyuabot.backend.bus.service.BusStopService
import app.hyuabot.backend.bus.service.BusTimetableService
import app.hyuabot.backend.database.entity.BusDepartureLog
import app.hyuabot.backend.database.entity.BusRealtime
import app.hyuabot.backend.database.entity.BusRoute
import app.hyuabot.backend.database.entity.BusRouteStop
import app.hyuabot.backend.database.entity.BusStop
import app.hyuabot.backend.database.entity.BusTimetable
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BusControllerTest {
    @MockitoBean
    private lateinit var routeService: BusRouteService

    @MockitoBean
    private lateinit var stopService: BusStopService

    @MockitoBean
    private lateinit var timetableService: BusTimetableService

    @MockitoBean
    private lateinit var realtimeService: BusRealtimeService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    companion object {
        val TEST_ROUTE_1 =
            BusRoute(
                id = 216000068,
                name = "10-1",
                typeCode = "13",
                typeName = "일반형시내버스",
                startStopID = 216000358,
                endStopID = 216000138,
                upFirstTime = LocalTime.parse("05:30"),
                upLastTime = LocalTime.parse("23:10"),
                downFirstTime = LocalTime.parse("06:00"),
                downLastTime = LocalTime.parse("23:40"),
                districtCode = 2,
                companyID = 4100700,
                companyName = "경원여객",
                companyPhone = "031-492-2260",
                stop = emptyList(),
            )
        val TEST_STOP_1 =
            BusStop(
                id = 216000358,
                name = "해솔초등학교",
                districtCode = 2,
                latitude = 37.794769,
                longitude = 127.074569,
                mobileNumber = "00000",
                regionName = "경기",
                busRoutes = emptyList(),
                startBusRoutes = emptyList(),
            )
        val TEST_STOP_2 =
            BusStop(
                id = 216000138,
                name = "상록수역3번출구건너편",
                districtCode = 2,
                latitude = 37.784939,
                longitude = 127.050743,
                mobileNumber = "00000",
                regionName = "경기",
                busRoutes = emptyList(),
                startBusRoutes = emptyList(),
            )
        val TEST_ROUTE_STOP_1 =
            BusRouteStop(
                seq = 1,
                route = TEST_ROUTE_1,
                stop = TEST_STOP_1,
                order = 1,
                routeID = TEST_ROUTE_1.id,
                stopID = TEST_STOP_1.id,
                startStopID = TEST_STOP_1.id,
                minuteFromStart = 0,
                startStop = TEST_STOP_1,
                log = emptyList(),
                realtime = emptyList(),
            )
        val TEST_ROUTE_STOP_2 =
            BusRouteStop(
                seq = 2,
                route = TEST_ROUTE_1,
                stop = TEST_STOP_2,
                order = 2,
                routeID = TEST_ROUTE_1.id,
                stopID = TEST_STOP_2.id,
                startStopID = TEST_STOP_1.id,
                minuteFromStart = 10,
                startStop = TEST_STOP_1,
                log = emptyList(),
                realtime = emptyList(),
            )
    }

    @Test
    @DisplayName("버스 노선 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetBusRoutes() {
        doReturn(
            listOf(
                BusServiceTest.TEST_ROUTE_1,
                BusServiceTest.TEST_ROUTE_2,
            ),
        ).whenever(routeService).getBusRouteList()
        mockMvc
            .perform(get("/api/v1/bus/route"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.result.length()").value(2))
            .andExpect(jsonPath("$.result[0].id").value(BusServiceTest.TEST_ROUTE_1.id))
            .andExpect(jsonPath("$.result[0].name").value(BusServiceTest.TEST_ROUTE_1.name))
            .andExpect(jsonPath("$.result[0].typeCode").value(BusServiceTest.TEST_ROUTE_1.typeCode))
            .andExpect(jsonPath("$.result[0].typeName").value(BusServiceTest.TEST_ROUTE_1.typeName))
            .andExpect(jsonPath("$.result[0].startStopID").value(BusServiceTest.TEST_ROUTE_1.startStopID))
            .andExpect(jsonPath("$.result[0].endStopID").value(BusServiceTest.TEST_ROUTE_1.endStopID))
            .andExpect(jsonPath("$.result[0].upFirstTime").value("05:30:00"))
            .andExpect(jsonPath("$.result[0].upLastTime").value("23:10:00"))
            .andExpect(jsonPath("$.result[0].downFirstTime").value("06:00:00"))
            .andExpect(jsonPath("$.result[0].downLastTime").value("23:40:00"))
            .andExpect(jsonPath("$.result[0].companyID").value(BusServiceTest.TEST_ROUTE_1.companyID))
            .andExpect(jsonPath("$.result[0].districtCode").value(BusServiceTest.TEST_ROUTE_1.districtCode))
    }

    @Test
    @DisplayName("버스 노선 생성")
    @WithCustomMockUser(username = "test_user")
    fun testCreateBusRoute() {
        val newRoute =
            BusRoute(
                id = 216000070,
                name = "10-3",
                typeCode = "13",
                typeName = "일반형시내버스",
                startStopID = 216000358,
                endStopID = 216000138,
                upFirstTime = LocalTime.parse("05:30"),
                upLastTime = LocalTime.parse("23:10"),
                downFirstTime = LocalTime.parse("06:00"),
                downLastTime = LocalTime.parse("23:40"),
                districtCode = 2,
                companyID = 4100700,
                companyName = "경원여객",
                companyPhone = "031-492-2260",
                stop = emptyList(),
            )
        val payload =
            CreateBusRouteRequest(
                id = 216000138,
                name = newRoute.name,
                typeCode = newRoute.typeCode,
                typeName = newRoute.typeName,
                startStopID = newRoute.startStopID,
                endStopID = newRoute.endStopID,
                upFirstTime = newRoute.upFirstTime.toString(),
                upLastTime = newRoute.upLastTime.toString(),
                downFirstTime = newRoute.downFirstTime.toString(),
                downLastTime = newRoute.downLastTime.toString(),
                districtCode = newRoute.districtCode,
                companyID = newRoute.companyID,
                companyName = newRoute.companyName,
                companyPhone = newRoute.companyPhone,
            )
        whenever(routeService.createBusRoute(payload)).thenReturn(newRoute)
        mockMvc
            .perform(
                post("/api/v1/bus/route")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(newRoute.id))
            .andExpect(jsonPath("$.name").value(newRoute.name))
            .andExpect(jsonPath("$.typeCode").value(newRoute.typeCode))
            .andExpect(jsonPath("$.typeName").value(newRoute.typeName))
            .andExpect(jsonPath("$.startStopID").value(newRoute.startStopID))
            .andExpect(jsonPath("$.endStopID").value(newRoute.endStopID))
            .andExpect(jsonPath("$.upFirstTime").value("05:30:00"))
            .andExpect(jsonPath("$.upLastTime").value("23:10:00"))
            .andExpect(jsonPath("$.downFirstTime").value("06:00:00"))
            .andExpect(jsonPath("$.downLastTime").value("23:40:00"))
            .andExpect(jsonPath("$.companyID").value(newRoute.companyID))
            .andExpect(jsonPath("$.districtCode").value(newRoute.districtCode))
    }

    @Test
    @DisplayName("버스 노선 생성 - 중복된 노선 ID")
    @WithCustomMockUser(username = "test_user")
    fun testCreateBusRouteDuplicateRouteID() {
        val payload =
            CreateBusRouteRequest(
                id = BusServiceTest.TEST_ROUTE_1.id,
                name = BusServiceTest.TEST_ROUTE_1.name,
                typeCode = BusServiceTest.TEST_ROUTE_1.typeCode,
                typeName = BusServiceTest.TEST_ROUTE_1.typeName,
                startStopID = BusServiceTest.TEST_ROUTE_1.startStopID,
                endStopID = BusServiceTest.TEST_ROUTE_1.endStopID,
                upFirstTime = BusServiceTest.TEST_ROUTE_1.upFirstTime.toString(),
                upLastTime = BusServiceTest.TEST_ROUTE_1.upLastTime.toString(),
                downFirstTime = BusServiceTest.TEST_ROUTE_1.downFirstTime.toString(),
                downLastTime = BusServiceTest.TEST_ROUTE_1.downLastTime.toString(),
                districtCode = BusServiceTest.TEST_ROUTE_1.districtCode,
                companyID = BusServiceTest.TEST_ROUTE_1.companyID,
                companyName = BusServiceTest.TEST_ROUTE_1.companyName,
                companyPhone = BusServiceTest.TEST_ROUTE_1.companyPhone,
            )
        doThrow(DuplicateBusRouteException()).whenever(routeService).createBusRoute(payload)
        mockMvc
            .perform(
                post("/api/v1/bus/route")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("DUPLICATE_BUS_ROUTE"))
    }

    @Test
    @DisplayName("버스 노선 생성 - 존재하지 않는 시점 정류장")
    @WithCustomMockUser(username = "test_user")
    fun testCreateBusRouteStartStopNotFound() {
        val payload =
            CreateBusRouteRequest(
                id = 216000070,
                name = BusServiceTest.TEST_ROUTE_1.name,
                typeCode = BusServiceTest.TEST_ROUTE_1.typeCode,
                typeName = BusServiceTest.TEST_ROUTE_1.typeName,
                startStopID = 999999999,
                endStopID = BusServiceTest.TEST_ROUTE_1.endStopID,
                upFirstTime = BusServiceTest.TEST_ROUTE_1.upFirstTime.toString(),
                upLastTime = BusServiceTest.TEST_ROUTE_1.upLastTime.toString(),
                downFirstTime = BusServiceTest.TEST_ROUTE_1.downFirstTime.toString(),
                downLastTime = BusServiceTest.TEST_ROUTE_1.downLastTime.toString(),
                districtCode = BusServiceTest.TEST_ROUTE_1.districtCode,
                companyID = BusServiceTest.TEST_ROUTE_1.companyID,
                companyName = BusServiceTest.TEST_ROUTE_1.companyName,
                companyPhone = BusServiceTest.TEST_ROUTE_1.companyPhone,
            )
        doThrow(BusStartStopNotFoundException()).whenever(routeService).createBusRoute(payload)
        mockMvc
            .perform(
                post("/api/v1/bus/route")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("BUS_START_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 노선 생성 - 존재하지 않는 종점 정류장")
    @WithCustomMockUser(username = "test_user")
    fun testCreateBusRouteEndStopNotFound() {
        val payload =
            CreateBusRouteRequest(
                id = 216000070,
                name = BusServiceTest.TEST_ROUTE_1.name,
                typeCode = BusServiceTest.TEST_ROUTE_1.typeCode,
                typeName = BusServiceTest.TEST_ROUTE_1.typeName,
                startStopID = BusServiceTest.TEST_ROUTE_1.startStopID,
                endStopID = 999999999,
                upFirstTime = BusServiceTest.TEST_ROUTE_1.upFirstTime.toString(),
                upLastTime = BusServiceTest.TEST_ROUTE_1.upLastTime.toString(),
                downFirstTime = BusServiceTest.TEST_ROUTE_1.downFirstTime.toString(),
                downLastTime = BusServiceTest.TEST_ROUTE_1.downLastTime.toString(),
                districtCode = BusServiceTest.TEST_ROUTE_1.districtCode,
                companyID = BusServiceTest.TEST_ROUTE_1.companyID,
                companyName = BusServiceTest.TEST_ROUTE_1.companyName,
                companyPhone = BusServiceTest.TEST_ROUTE_1.companyPhone,
            )
        doThrow(BusEndStopNotFoundException()).whenever(routeService).createBusRoute(payload)
        mockMvc
            .perform(
                post("/api/v1/bus/route")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("BUS_END_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 노선 생성 - 시간 형식 오류")
    @WithCustomMockUser(username = "test_user")
    fun testCreateBusRouteTimeFormatError() {
        val payload =
            CreateBusRouteRequest(
                id = 216000070,
                name = BusServiceTest.TEST_ROUTE_1.name,
                typeCode = BusServiceTest.TEST_ROUTE_1.typeCode,
                typeName = BusServiceTest.TEST_ROUTE_1.typeName,
                startStopID = BusServiceTest.TEST_ROUTE_1.startStopID,
                endStopID = BusServiceTest.TEST_ROUTE_1.endStopID,
                upFirstTime = "invalid_time",
                upLastTime = BusServiceTest.TEST_ROUTE_1.upLastTime.toString(),
                downFirstTime = BusServiceTest.TEST_ROUTE_1.downFirstTime.toString(),
                downLastTime = BusServiceTest.TEST_ROUTE_1.downLastTime.toString(),
                districtCode = BusServiceTest.TEST_ROUTE_1.districtCode,
                companyID = BusServiceTest.TEST_ROUTE_1.companyID,
                companyName = BusServiceTest.TEST_ROUTE_1.companyName,
                companyPhone = BusServiceTest.TEST_ROUTE_1.companyPhone,
            )
        doThrow(LocalTimeNotValidException()).whenever(routeService).createBusRoute(payload)
        mockMvc
            .perform(
                post("/api/v1/bus/route")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("LOCAL_TIME_NOT_VALID"))
    }

    @Test
    @DisplayName("버스 노선 생성 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testCreateBusRouteOtherError() {
        val payload =
            CreateBusRouteRequest(
                id = 216000070,
                name = BusServiceTest.TEST_ROUTE_1.name,
                typeCode = BusServiceTest.TEST_ROUTE_1.typeCode,
                typeName = BusServiceTest.TEST_ROUTE_1.typeName,
                startStopID = BusServiceTest.TEST_ROUTE_1.startStopID,
                endStopID = BusServiceTest.TEST_ROUTE_1.endStopID,
                upFirstTime = BusServiceTest.TEST_ROUTE_1.upFirstTime.toString(),
                upLastTime = BusServiceTest.TEST_ROUTE_1.upLastTime.toString(),
                downFirstTime = BusServiceTest.TEST_ROUTE_1.downFirstTime.toString(),
                downLastTime = BusServiceTest.TEST_ROUTE_1.downLastTime.toString(),
                districtCode = BusServiceTest.TEST_ROUTE_1.districtCode,
                companyID = BusServiceTest.TEST_ROUTE_1.companyID,
                companyName = BusServiceTest.TEST_ROUTE_1.companyName,
                companyPhone = BusServiceTest.TEST_ROUTE_1.companyPhone,
            )
        doThrow(RuntimeException()).whenever(routeService).createBusRoute(payload)
        mockMvc
            .perform(
                post("/api/v1/bus/route")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("버스 노선 상세 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetBusRouteById() {
        doReturn(TEST_ROUTE_1).whenever(routeService).getBusRouteById(TEST_ROUTE_1.id)
        mockMvc
            .perform(get("/api/v1/bus/route/${TEST_ROUTE_1.id}"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(TEST_ROUTE_1.id))
            .andExpect(jsonPath("$.name").value(TEST_ROUTE_1.name))
            .andExpect(jsonPath("$.typeCode").value(TEST_ROUTE_1.typeCode))
            .andExpect(jsonPath("$.typeName").value(TEST_ROUTE_1.typeName))
            .andExpect(jsonPath("$.startStopID").value(TEST_ROUTE_1.startStopID))
            .andExpect(jsonPath("$.endStopID").value(TEST_ROUTE_1.endStopID))
            .andExpect(jsonPath("$.upFirstTime").value("05:30:00"))
            .andExpect(jsonPath("$.upLastTime").value("23:10:00"))
            .andExpect(jsonPath("$.downFirstTime").value("06:00:00"))
            .andExpect(jsonPath("$.downLastTime").value("23:40:00"))
            .andExpect(jsonPath("$.companyID").value(TEST_ROUTE_1.companyID))
            .andExpect(jsonPath("$.districtCode").value(TEST_ROUTE_1.districtCode))
    }

    @Test
    @DisplayName("버스 노선 상세 조회 - 존재하지 않는 노선")
    @WithCustomMockUser(username = "test_user")
    fun testGetBusRouteByIdNotFound() {
        doThrow(BusRouteNotFoundException()).whenever(routeService).getBusRouteById(999999999)
        mockMvc
            .perform(get("/api/v1/bus/route/999999999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("BUS_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 노선 상세 조회 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testGetBusRouteByIdOtherError() {
        doThrow(RuntimeException()).whenever(routeService).getBusRouteById(999999999)
        mockMvc
            .perform(get("/api/v1/bus/route/999999999"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("버스 노선 수정")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBusRoute() {
        val updatedRoute =
            BusRoute(
                id = TEST_ROUTE_1.id,
                name = "10-1-1",
                typeCode = "13",
                typeName = "일반형시내버스",
                startStopID = TEST_ROUTE_1.startStopID,
                endStopID = TEST_ROUTE_1.endStopID,
                upFirstTime = LocalTime.parse("05:30"),
                upLastTime = LocalTime.parse("23:10"),
                downFirstTime = LocalTime.parse("06:00"),
                downLastTime = LocalTime.parse("23:40"),
                districtCode = 2,
                companyID = 4100700,
                companyName = "경원여객",
                companyPhone = "031-492-2260",
                stop = emptyList(),
            )
        val payload =
            UpdateBusRouteRequest(
                name = updatedRoute.name,
                typeCode = updatedRoute.typeCode,
                typeName = updatedRoute.typeName,
                startStopID = updatedRoute.startStopID,
                endStopID = updatedRoute.endStopID,
                upFirstTime = updatedRoute.upFirstTime.toString(),
                upLastTime = updatedRoute.upLastTime.toString(),
                downFirstTime = updatedRoute.downFirstTime.toString(),
                downLastTime = updatedRoute.downLastTime.toString(),
                districtCode = updatedRoute.districtCode,
                companyID = updatedRoute.companyID,
                companyName = updatedRoute.companyName,
                companyPhone = updatedRoute.companyPhone,
            )
        whenever(routeService.updateBusRoute(TEST_ROUTE_1.id, payload)).thenReturn(updatedRoute)
        mockMvc
            .perform(
                put("/api/v1/bus/route/${TEST_ROUTE_1.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(updatedRoute.id))
            .andExpect(jsonPath("$.name").value(updatedRoute.name))
            .andExpect(jsonPath("$.typeCode").value(updatedRoute.typeCode))
            .andExpect(jsonPath("$.typeName").value(updatedRoute.typeName))
            .andExpect(jsonPath("$.startStopID").value(updatedRoute.startStopID))
            .andExpect(jsonPath("$.endStopID").value(updatedRoute.endStopID))
            .andExpect(jsonPath("$.upFirstTime").value("05:30:00"))
            .andExpect(jsonPath("$.upLastTime").value("23:10:00"))
            .andExpect(jsonPath("$.downFirstTime").value("06:00:00"))
            .andExpect(jsonPath("$.downLastTime").value("23:40:00"))
            .andExpect(jsonPath("$.companyID").value(updatedRoute.companyID))
            .andExpect(jsonPath("$.districtCode").value(updatedRoute.districtCode))
    }

    @Test
    @DisplayName("버스 노선 수정 - 존재하지 않는 노선")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBusRouteNotFound() {
        val payload =
            UpdateBusRouteRequest(
                name = TEST_ROUTE_1.name,
                typeCode = TEST_ROUTE_1.typeCode,
                typeName = TEST_ROUTE_1.typeName,
                startStopID = TEST_ROUTE_1.startStopID,
                endStopID = TEST_ROUTE_1.endStopID,
                upFirstTime = TEST_ROUTE_1.upFirstTime.toString(),
                upLastTime = TEST_ROUTE_1.upLastTime.toString(),
                downFirstTime = TEST_ROUTE_1.downFirstTime.toString(),
                downLastTime = TEST_ROUTE_1.downLastTime.toString(),
                districtCode = TEST_ROUTE_1.districtCode,
                companyID = TEST_ROUTE_1.companyID,
                companyName = TEST_ROUTE_1.companyName,
                companyPhone = TEST_ROUTE_1.companyPhone,
            )
        doThrow(BusRouteNotFoundException()).whenever(routeService).updateBusRoute(999999999, payload)
        mockMvc
            .perform(
                put("/api/v1/bus/route/999999999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("BUS_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 노선 수정 - 존재하지 않는 시점 정류장")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBusRouteStartStopNotFound() {
        val payload =
            UpdateBusRouteRequest(
                name = TEST_ROUTE_1.name,
                typeCode = TEST_ROUTE_1.typeCode,
                typeName = TEST_ROUTE_1.typeName,
                startStopID = 999999999,
                endStopID = TEST_ROUTE_1.endStopID,
                upFirstTime = TEST_ROUTE_1.upFirstTime.toString(),
                upLastTime = TEST_ROUTE_1.upLastTime.toString(),
                downFirstTime = TEST_ROUTE_1.downFirstTime.toString(),
                downLastTime = TEST_ROUTE_1.downLastTime.toString(),
                districtCode = TEST_ROUTE_1.districtCode,
                companyID = TEST_ROUTE_1.companyID,
                companyName = TEST_ROUTE_1.companyName,
                companyPhone = TEST_ROUTE_1.companyPhone,
            )
        doThrow(BusStartStopNotFoundException()).whenever(routeService).updateBusRoute(TEST_ROUTE_1.id, payload)
        mockMvc
            .perform(
                put("/api/v1/bus/route/${TEST_ROUTE_1.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("BUS_START_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 노선 수정 - 존재하지 않는 종점 정류장")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBusRouteEndStopNotFound() {
        val payload =
            UpdateBusRouteRequest(
                name = TEST_ROUTE_1.name,
                typeCode = TEST_ROUTE_1.typeCode,
                typeName = TEST_ROUTE_1.typeName,
                startStopID = TEST_ROUTE_1.startStopID,
                endStopID = 999999999,
                upFirstTime = TEST_ROUTE_1.upFirstTime.toString(),
                upLastTime = TEST_ROUTE_1.upLastTime.toString(),
                downFirstTime = TEST_ROUTE_1.downFirstTime.toString(),
                downLastTime = TEST_ROUTE_1.downLastTime.toString(),
                districtCode = TEST_ROUTE_1.districtCode,
                companyID = TEST_ROUTE_1.companyID,
                companyName = TEST_ROUTE_1.companyName,
                companyPhone = TEST_ROUTE_1.companyPhone,
            )
        doThrow(BusEndStopNotFoundException()).whenever(routeService).updateBusRoute(TEST_ROUTE_1.id, payload)
        mockMvc
            .perform(
                put("/api/v1/bus/route/${TEST_ROUTE_1.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("BUS_END_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 노선 수정 - 시간 형식 오류")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBusRouteTimeFormatError() {
        val payload =
            UpdateBusRouteRequest(
                name = TEST_ROUTE_1.name,
                typeCode = TEST_ROUTE_1.typeCode,
                typeName = TEST_ROUTE_1.typeName,
                startStopID = TEST_ROUTE_1.startStopID,
                endStopID = TEST_ROUTE_1.endStopID,
                upFirstTime = "invalid_time",
                upLastTime = TEST_ROUTE_1.upLastTime.toString(),
                downFirstTime = TEST_ROUTE_1.downFirstTime.toString(),
                downLastTime = TEST_ROUTE_1.downLastTime.toString(),
                districtCode = TEST_ROUTE_1.districtCode,
                companyID = TEST_ROUTE_1.companyID,
                companyName = TEST_ROUTE_1.companyName,
                companyPhone = TEST_ROUTE_1.companyPhone,
            )
        doThrow(LocalTimeNotValidException()).whenever(routeService).updateBusRoute(TEST_ROUTE_1.id, payload)
        mockMvc
            .perform(
                put("/api/v1/bus/route/${TEST_ROUTE_1.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("LOCAL_TIME_NOT_VALID"))
    }

    @Test
    @DisplayName("버스 노선 수정 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBusRouteOtherError() {
        val payload =
            UpdateBusRouteRequest(
                name = TEST_ROUTE_1.name,
                typeCode = TEST_ROUTE_1.typeCode,
                typeName = TEST_ROUTE_1.typeName,
                startStopID = TEST_ROUTE_1.startStopID,
                endStopID = TEST_ROUTE_1.endStopID,
                upFirstTime = TEST_ROUTE_1.upFirstTime.toString(),
                upLastTime = TEST_ROUTE_1.upLastTime.toString(),
                downFirstTime = TEST_ROUTE_1.downFirstTime.toString(),
                downLastTime = TEST_ROUTE_1.downLastTime.toString(),
                districtCode = TEST_ROUTE_1.districtCode,
                companyID = TEST_ROUTE_1.companyID,
                companyName = TEST_ROUTE_1.companyName,
                companyPhone = TEST_ROUTE_1.companyPhone,
            )
        doThrow(RuntimeException()).whenever(routeService).updateBusRoute(TEST_ROUTE_1.id, payload)
        mockMvc
            .perform(
                put("/api/v1/bus/route/${TEST_ROUTE_1.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("버스 노선 삭제")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteBusRoute() {
        mockMvc
            .perform(
                delete("/api/v1/bus/route/${TEST_ROUTE_1.id}"),
            ).andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("버스 노선 삭제 - 존재하지 않는 노선")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteBusRouteNotFound() {
        doThrow(BusRouteNotFoundException()).whenever(routeService).deleteBusRouteById(999999999)
        mockMvc
            .perform(delete("/api/v1/bus/route/999999999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("BUS_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 노선 삭제 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteBusRouteOtherError() {
        doThrow(RuntimeException()).whenever(routeService).deleteBusRouteById(999999999)
        mockMvc
            .perform(delete("/api/v1/bus/route/999999999"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("버스 노선 정류장 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetBusRouteStops() {
        doReturn(listOf(TEST_ROUTE_STOP_1, TEST_ROUTE_STOP_2))
            .whenever(routeService)
            .getBusStopListByRouteID(TEST_ROUTE_1.id)
        mockMvc
            .perform(get("/api/v1/bus/route/${TEST_ROUTE_1.id}/stop"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.result.length()").value(2))
            .andExpect(jsonPath("$.result[0].seq").value(TEST_ROUTE_STOP_1.seq))
            .andExpect(jsonPath("$.result[0].routeID").value(TEST_ROUTE_STOP_1.routeID))
            .andExpect(jsonPath("$.result[0].stopID").value(TEST_ROUTE_STOP_1.stopID))
            .andExpect(jsonPath("$.result[0].order").value(TEST_ROUTE_STOP_1.order))
            .andExpect(jsonPath("$.result[0].startStopID").value(TEST_ROUTE_STOP_1.startStopID))
            .andExpect(jsonPath("$.result[0].travelTime").value(TEST_ROUTE_STOP_1.minuteFromStart))
            .andExpect(jsonPath("$.result[1].seq").value(TEST_ROUTE_STOP_2.seq))
            .andExpect(jsonPath("$.result[1].routeID").value(TEST_ROUTE_STOP_2.routeID))
            .andExpect(jsonPath("$.result[1].stopID").value(TEST_ROUTE_STOP_2.stopID))
            .andExpect(jsonPath("$.result[1].order").value(TEST_ROUTE_STOP_2.order))
            .andExpect(jsonPath("$.result[1].startStopID").value(TEST_ROUTE_STOP_2.startStopID))
            .andExpect(jsonPath("$.result[1].travelTime").value(TEST_ROUTE_STOP_2.minuteFromStart))
    }

    @Test
    @DisplayName("버스 노선 정류장 목록 조회 - 존재하지 않는 노선")
    @WithCustomMockUser(username = "test_user")
    fun testGetBusRouteStopsRouteNotFound() {
        doThrow(BusRouteNotFoundException()).whenever(routeService).getBusStopListByRouteID(999999999)
        mockMvc
            .perform(get("/api/v1/bus/route/999999999/stop"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("BUS_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 노선 정류장 목록 조회 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testGetBusRouteStopsOtherError() {
        doThrow(RuntimeException()).whenever(routeService).getBusStopListByRouteID(999999999)
        mockMvc
            .perform(get("/api/v1/bus/route/999999999/stop"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("버스 노선 정류장 추가")
    @WithCustomMockUser(username = "test_user")
    fun testAddBusRouteStop() {
        val newRouteStop =
            BusRouteStop(
                seq = 3,
                route = TEST_ROUTE_1,
                stop = BusServiceTest.TEST_STOP_2,
                order = 3,
                routeID = TEST_ROUTE_1.id,
                stopID = TEST_STOP_2.id,
                startStopID = TEST_STOP_1.id,
                minuteFromStart = 20,
                startStop = TEST_STOP_1,
                log = emptyList(),
                realtime = emptyList(),
            )
        val payload =
            BusRouteStopRequest(
                stopID = newRouteStop.stopID,
                order = newRouteStop.order,
                startStopID = newRouteStop.startStopID,
                travelTime = newRouteStop.minuteFromStart,
            )
        whenever(routeService.createBusRouteStop(TEST_ROUTE_1.id, payload)).thenReturn(newRouteStop)
        mockMvc
            .perform(
                post("/api/v1/bus/route/${TEST_ROUTE_1.id}/stop")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.seq").value(newRouteStop.seq))
            .andExpect(jsonPath("$.routeID").value(newRouteStop.routeID))
            .andExpect(jsonPath("$.stopID").value(newRouteStop.stopID))
            .andExpect(jsonPath("$.order").value(newRouteStop.order))
            .andExpect(jsonPath("$.startStopID").value(newRouteStop.startStopID))
            .andExpect(jsonPath("$.travelTime").value(newRouteStop.minuteFromStart))
    }

    @Test
    @DisplayName("버스 노선 정류장 추가 - 존재하지 않는 노선")
    @WithCustomMockUser(username = "test_user")
    fun testAddBusRouteStopRouteNotFound() {
        val payload =
            BusRouteStopRequest(
                stopID = BusServiceTest.TEST_STOP_1.id,
                order = 1,
                startStopID = BusServiceTest.TEST_STOP_1.id,
                travelTime = 0,
            )
        doThrow(BusRouteNotFoundException()).whenever(routeService).createBusRouteStop(999999999, payload)
        mockMvc
            .perform(
                post("/api/v1/bus/route/999999999/stop")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("BUS_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 노선 정류장 추가 - 존재하지 않는 정류장")
    @WithCustomMockUser(username = "test_user")
    fun testAddBusRouteStopStopNotFound() {
        val payload =
            BusRouteStopRequest(
                stopID = 999999999,
                order = 1,
                startStopID = BusServiceTest.TEST_STOP_1.id,
                travelTime = 0,
            )
        doThrow(BusStopNotFoundException()).whenever(routeService).createBusRouteStop(TEST_ROUTE_1.id, payload)
        mockMvc
            .perform(
                post("/api/v1/bus/route/${TEST_ROUTE_1.id}/stop")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("BUS_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 노선 정류장 추가 - 존재하지 않는 시점 정류장")
    @WithCustomMockUser(username = "test_user")
    fun testAddBusRouteStopStartStopNotFound() {
        val payload =
            BusRouteStopRequest(
                stopID = BusServiceTest.TEST_STOP_1.id,
                order = 1,
                startStopID = 999999999,
                travelTime = 0,
            )
        doThrow(BusStartStopNotFoundException()).whenever(routeService).createBusRouteStop(TEST_ROUTE_1.id, payload)
        mockMvc
            .perform(
                post("/api/v1/bus/route/${TEST_ROUTE_1.id}/stop")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("BUS_START_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 노선 정류장 추가 - 중복된 정류장 순서")
    @WithCustomMockUser(username = "test_user")
    fun testAddBusRouteStopDuplicateOrder() {
        val payload =
            BusRouteStopRequest(
                stopID = BusServiceTest.TEST_STOP_1.id,
                order = TEST_ROUTE_STOP_1.order,
                startStopID = BusServiceTest.TEST_STOP_1.id,
                travelTime = 0,
            )
        doThrow(DuplicateBusRouteStopException()).whenever(routeService).createBusRouteStop(TEST_ROUTE_1.id, payload)
        mockMvc
            .perform(
                post("/api/v1/bus/route/${TEST_ROUTE_1.id}/stop")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("DUPLICATE_BUS_ROUTE_STOP"))
    }

    @Test
    @DisplayName("버스 노선 정류장 추가 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testAddBusRouteStopOtherError() {
        val payload =
            BusRouteStopRequest(
                stopID = BusServiceTest.TEST_STOP_1.id,
                order = 1,
                startStopID = BusServiceTest.TEST_STOP_1.id,
                travelTime = 0,
            )
        doThrow(RuntimeException()).whenever(routeService).createBusRouteStop(TEST_ROUTE_1.id, payload)
        mockMvc
            .perform(
                post("/api/v1/bus/route/${TEST_ROUTE_1.id}/stop")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("버스 노선 정류장 수정")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBusRouteStop() {
        val updatedRouteStop =
            BusRouteStop(
                seq = TEST_ROUTE_STOP_1.seq,
                route = TEST_ROUTE_1,
                stop = BusServiceTest.TEST_STOP_2,
                order = 10,
                routeID = TEST_ROUTE_1.id,
                stopID = TEST_STOP_2.id,
                startStopID = TEST_STOP_1.id,
                minuteFromStart = 30,
                startStop = TEST_STOP_1,
                log = emptyList(),
                realtime = emptyList(),
            )
        val payload =
            BusRouteStopRequest(
                stopID = updatedRouteStop.stopID,
                order = updatedRouteStop.order,
                startStopID = updatedRouteStop.startStopID,
                travelTime = updatedRouteStop.minuteFromStart,
            )
        whenever(routeService.updateBusRouteStop(TEST_ROUTE_1.id, TEST_ROUTE_STOP_1.seq!!, payload))
            .thenReturn(updatedRouteStop)
        mockMvc
            .perform(
                put("/api/v1/bus/route/${TEST_ROUTE_1.id}/stop/${TEST_ROUTE_STOP_1.seq}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.seq").value(updatedRouteStop.seq))
            .andExpect(jsonPath("$.routeID").value(updatedRouteStop.routeID))
            .andExpect(jsonPath("$.stopID").value(updatedRouteStop.stopID))
            .andExpect(jsonPath("$.order").value(updatedRouteStop.order))
            .andExpect(jsonPath("$.startStopID").value(updatedRouteStop.startStopID))
            .andExpect(jsonPath("$.travelTime").value(updatedRouteStop.minuteFromStart))
    }

    @Test
    @DisplayName("버스 노선 정류장 수정 - 존재하지 않는 노선")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBusRouteStopRouteNotFound() {
        val payload =
            BusRouteStopRequest(
                stopID = BusServiceTest.TEST_STOP_1.id,
                order = 1,
                startStopID = BusServiceTest.TEST_STOP_1.id,
                travelTime = 0,
            )
        doThrow(BusRouteNotFoundException())
            .whenever(routeService)
            .updateBusRouteStop(999999999, TEST_ROUTE_STOP_1.seq!!, payload)
        mockMvc
            .perform(
                put("/api/v1/bus/route/999999999/stop/${TEST_ROUTE_STOP_1.seq}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("BUS_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 노선 정류장 수정 - 존재하지 않는 노선 - 정류장 Seq")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBusRouteStopSeqNotFound() {
        val payload =
            BusRouteStopRequest(
                stopID = BusServiceTest.TEST_STOP_1.id,
                order = 1,
                startStopID = BusServiceTest.TEST_STOP_1.id,
                travelTime = 0,
            )
        doThrow(BusRouteStopNotFoundException())
            .whenever(routeService)
            .updateBusRouteStop(TEST_ROUTE_1.id, 999999999, payload)
        mockMvc
            .perform(
                put("/api/v1/bus/route/${TEST_ROUTE_1.id}/stop/999999999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("BUS_ROUTE_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 노선 정류장 수정 - 존재하지 않는 정류장")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBusRouteStopStopNotFound() {
        val payload =
            BusRouteStopRequest(
                stopID = 999999999,
                order = 1,
                startStopID = BusServiceTest.TEST_STOP_1.id,
                travelTime = 0,
            )
        doThrow(BusStopNotFoundException())
            .whenever(routeService)
            .updateBusRouteStop(TEST_ROUTE_1.id, TEST_ROUTE_STOP_1.seq!!, payload)
        mockMvc
            .perform(
                put("/api/v1/bus/route/${TEST_ROUTE_1.id}/stop/${TEST_ROUTE_STOP_1.seq}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("BUS_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 노선 정류장 수정 - 존재하지 않는 시점 정류장")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBusRouteStopStartStopNotFound() {
        val payload =
            BusRouteStopRequest(
                stopID = BusServiceTest.TEST_STOP_1.id,
                order = 1,
                startStopID = 999999999,
                travelTime = 0,
            )
        doThrow(BusStartStopNotFoundException())
            .whenever(routeService)
            .updateBusRouteStop(TEST_ROUTE_1.id, TEST_ROUTE_STOP_1.seq!!, payload)
        mockMvc
            .perform(
                put("/api/v1/bus/route/${TEST_ROUTE_1.id}/stop/${TEST_ROUTE_STOP_1.seq}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("BUS_START_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 노선 정류장 수정 - 중복된 정류장 순서")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBusRouteStopDuplicateOrder() {
        val payload =
            BusRouteStopRequest(
                stopID = BusServiceTest.TEST_STOP_1.id,
                order = TEST_ROUTE_STOP_2.order,
                startStopID = BusServiceTest.TEST_STOP_1.id,
                travelTime = 0,
            )
        doThrow(DuplicateBusRouteStopException())
            .whenever(routeService)
            .updateBusRouteStop(TEST_ROUTE_1.id, TEST_ROUTE_STOP_1.seq!!, payload)
        mockMvc
            .perform(
                put("/api/v1/bus/route/${TEST_ROUTE_1.id}/stop/${TEST_ROUTE_STOP_1.seq}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("DUPLICATE_BUS_ROUTE_STOP"))
    }

    @Test
    @DisplayName("버스 노선 정류장 수정 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBusRouteStopOtherError() {
        val payload =
            BusRouteStopRequest(
                stopID = BusServiceTest.TEST_STOP_1.id,
                order = 1,
                startStopID = BusServiceTest.TEST_STOP_1.id,
                travelTime = 0,
            )
        doThrow(RuntimeException())
            .whenever(routeService)
            .updateBusRouteStop(TEST_ROUTE_1.id, TEST_ROUTE_STOP_1.seq!!, payload)
        mockMvc
            .perform(
                put("/api/v1/bus/route/${TEST_ROUTE_1.id}/stop/${TEST_ROUTE_STOP_1.seq}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("버스 노선 정류장 삭제")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteBusRouteStop() {
        mockMvc
            .perform(
                delete("/api/v1/bus/route/${TEST_ROUTE_1.id}/stop/${TEST_ROUTE_STOP_1.seq}"),
            ).andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("버스 노선 정류장 삭제 - 존재하지 않는 노선")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteBusRouteStopRouteNotFound() {
        doThrow(BusRouteNotFoundException()).whenever(routeService).deleteBusRouteStopBySeq(999999999, TEST_ROUTE_STOP_1.seq!!)
        mockMvc
            .perform(delete("/api/v1/bus/route/999999999/stop/${TEST_ROUTE_STOP_1.seq}"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("BUS_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 노선 정류장 삭제 - 존재하지 않는 노선 - 정류장 Seq")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteBusRouteStopSeqNotFound() {
        doThrow(BusRouteStopNotFoundException()).whenever(routeService).deleteBusRouteStopBySeq(TEST_ROUTE_1.id, 999999999)
        mockMvc
            .perform(delete("/api/v1/bus/route/${TEST_ROUTE_1.id}/stop/999999999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("BUS_ROUTE_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 노선 정류장 삭제 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteBusRouteStopOtherError() {
        doThrow(RuntimeException()).whenever(routeService).deleteBusRouteStopBySeq(TEST_ROUTE_1.id, TEST_ROUTE_STOP_1.seq!!)
        mockMvc
            .perform(delete("/api/v1/bus/route/${TEST_ROUTE_1.id}/stop/${TEST_ROUTE_STOP_1.seq}"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("버스 정류장의 특정 노선 도착 기록 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetBusStopRouteLogs() {
        doReturn(
            listOf(
                BusDepartureLog(
                    seq = 1,
                    routeID = 216000068,
                    stopID = 216000138,
                    departureDate = LocalDate.parse("2021-03-15"),
                    departureTime = LocalTime.parse("14:30:00"),
                    vehicleID = "123가4567",
                    routeStop = null,
                ),
                BusDepartureLog(
                    seq = 2,
                    routeID = 216000068,
                    stopID = 216000138,
                    departureDate = LocalDate.parse("2021-03-15"),
                    departureTime = LocalTime.parse("15:00:00"),
                    vehicleID = "234나5678",
                    routeStop = null,
                ),
            ),
        ).whenever(routeService).getBusDepartureLogByRouteStop(TEST_ROUTE_1.id, TEST_STOP_1.id)
        mockMvc
            .perform(get("/api/v1/bus/route/${TEST_ROUTE_1.id}/stop/${TEST_STOP_1.id}/log"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.result.length()").value(2))
            .andExpect(jsonPath("$.result[0].routeID").value(216000068))
            .andExpect(jsonPath("$.result[0].stopID").value(216000138))
    }

    @Test
    @DisplayName("버스 정류장의 특정 노선 도착 기록 조회 - 존재하지 않는 노선")
    @WithCustomMockUser(username = "test_user")
    fun testGetBusStopRouteLogsRouteNotFound() {
        doThrow(BusRouteNotFoundException()).whenever(routeService).getBusDepartureLogByRouteStop(999999999, TEST_STOP_1.id)
        mockMvc
            .perform(get("/api/v1/bus/route/999999999/stop/${TEST_STOP_1.id}/log"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("BUS_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 정류장의 특정 노선 도착 기록 조회 - 존재하지 않는 정류장")
    @WithCustomMockUser(username = "test_user")
    fun testGetBusStopRouteLogsStopNotFound() {
        doThrow(BusRouteStopNotFoundException()).whenever(routeService).getBusDepartureLogByRouteStop(TEST_ROUTE_1.id, 999999999)
        mockMvc
            .perform(get("/api/v1/bus/route/${TEST_ROUTE_1.id}/stop/999999999/log"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("BUS_ROUTE_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 정류장의 특정 노선 도착 기록 조회 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testGetBusStopRouteLogsOtherError() {
        doThrow(RuntimeException()).whenever(routeService).getBusDepartureLogByRouteStop(TEST_ROUTE_1.id, TEST_STOP_1.id)
        mockMvc
            .perform(get("/api/v1/bus/route/${TEST_ROUTE_1.id}/stop/${TEST_STOP_1.id}/log"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("버스 정류장 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetBusStops() {
        doReturn(listOf(BusServiceTest.TEST_STOP_1, BusServiceTest.TEST_STOP_2))
            .whenever(stopService)
            .getBusStopList()
        mockMvc
            .perform(get("/api/v1/bus/stop"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.result.length()").value(2))
            .andExpect(jsonPath("$.result[0].id").value(BusServiceTest.TEST_STOP_1.id))
            .andExpect(jsonPath("$.result[0].name").value(BusServiceTest.TEST_STOP_1.name))
            .andExpect(jsonPath("$.result[0].latitude").value(BusServiceTest.TEST_STOP_1.latitude))
            .andExpect(jsonPath("$.result[0].longitude").value(BusServiceTest.TEST_STOP_1.longitude))
            .andExpect(jsonPath("$.result[1].id").value(BusServiceTest.TEST_STOP_2.id))
            .andExpect(jsonPath("$.result[1].name").value(BusServiceTest.TEST_STOP_2.name))
            .andExpect(jsonPath("$.result[1].latitude").value(BusServiceTest.TEST_STOP_2.latitude))
            .andExpect(jsonPath("$.result[1].longitude").value(BusServiceTest.TEST_STOP_2.longitude))
    }

    @Test
    @DisplayName("버스 정류장 생성")
    @WithCustomMockUser(username = "test_user")
    fun testCreateBusStop() {
        val payload =
            CreateBusStopRequest(
                id = TEST_STOP_1.id,
                name = TEST_STOP_1.name,
                latitude = TEST_STOP_1.latitude,
                longitude = TEST_STOP_1.longitude,
                mobileNumber = TEST_STOP_1.mobileNumber,
                districtCode = TEST_STOP_1.districtCode,
                regionName = TEST_STOP_1.regionName,
            )
        whenever(stopService.createBusStop(payload)).thenReturn(TEST_STOP_1)
        mockMvc
            .perform(
                post("/api/v1/bus/stop")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(TEST_STOP_1.id))
            .andExpect(jsonPath("$.name").value(TEST_STOP_1.name))
            .andExpect(jsonPath("$.latitude").value(TEST_STOP_1.latitude))
            .andExpect(jsonPath("$.longitude").value(TEST_STOP_1.longitude))
            .andExpect(jsonPath("$.mobileNumber").value(TEST_STOP_1.mobileNumber))
    }

    @Test
    @DisplayName("버스 정류장 생성 - 중복된 정류장 ID")
    @WithCustomMockUser(username = "test_user")
    fun testCreateBusStopDuplicateID() {
        val payload =
            CreateBusStopRequest(
                id = TEST_STOP_1.id,
                name = TEST_STOP_1.name,
                latitude = TEST_STOP_1.latitude,
                longitude = TEST_STOP_1.longitude,
                mobileNumber = TEST_STOP_1.mobileNumber,
                districtCode = TEST_STOP_1.districtCode,
                regionName = TEST_STOP_1.regionName,
            )
        doThrow(DuplicateBusStopException()).whenever(stopService).createBusStop(payload)
        mockMvc
            .perform(
                post("/api/v1/bus/stop")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("DUPLICATE_BUS_STOP"))
    }

    @Test
    @DisplayName("버스 정류장 생성 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testCreateBusStopOtherError() {
        val payload =
            CreateBusStopRequest(
                id = TEST_STOP_1.id,
                name = TEST_STOP_1.name,
                latitude = TEST_STOP_1.latitude,
                longitude = TEST_STOP_1.longitude,
                mobileNumber = TEST_STOP_1.mobileNumber,
                districtCode = TEST_STOP_1.districtCode,
                regionName = TEST_STOP_1.regionName,
            )
        doThrow(RuntimeException()).whenever(stopService).createBusStop(payload)
        mockMvc
            .perform(
                post("/api/v1/bus/stop")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("버스 정류장 항목 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetBusStop() {
        doReturn(TEST_STOP_1).whenever(stopService).getBusStopById(TEST_STOP_1.id)
        mockMvc
            .perform(get("/api/v1/bus/stop/${TEST_STOP_1.id}"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(TEST_STOP_1.id))
            .andExpect(jsonPath("$.name").value(TEST_STOP_1.name))
            .andExpect(jsonPath("$.latitude").value(TEST_STOP_1.latitude))
            .andExpect(jsonPath("$.longitude").value(TEST_STOP_1.longitude))
            .andExpect(jsonPath("$.mobileNumber").value(TEST_STOP_1.mobileNumber))
    }

    @Test
    @DisplayName("버스 정류장 항목 조회 - 존재하지 않는 정류장")
    @WithCustomMockUser(username = "test_user")
    fun testGetBusStopNotFound() {
        doThrow(BusStopNotFoundException()).whenever(stopService).getBusStopById(999999999)
        mockMvc
            .perform(get("/api/v1/bus/stop/999999999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("BUS_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 정류장 항목 조회 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testGetBusStopOtherError() {
        doThrow(RuntimeException()).whenever(stopService).getBusStopById(999999999)
        mockMvc
            .perform(get("/api/v1/bus/stop/999999999"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("버스 정류장 수정")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBusStop() {
        val payload =
            UpdateBusStopRequest(
                name = "Updated Stop Name",
                latitude = 37.123456,
                longitude = 127.123456,
                mobileNumber = "031-123-4567",
                districtCode = 210000123,
                regionName = "Updated Region",
            )
        val updatedStop =
            BusStop(
                id = TEST_STOP_1.id,
                name = payload.name,
                latitude = payload.latitude,
                longitude = payload.longitude,
                mobileNumber = payload.mobileNumber,
                districtCode = payload.districtCode,
                regionName = payload.regionName,
                busRoutes = emptyList(),
                startBusRoutes = emptyList(),
            )
        whenever(stopService.updateBusStop(TEST_STOP_1.id, payload)).thenReturn(updatedStop)
        mockMvc
            .perform(
                put("/api/v1/bus/stop/${TEST_STOP_1.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(updatedStop.id))
            .andExpect(jsonPath("$.name").value(updatedStop.name))
            .andExpect(jsonPath("$.latitude").value(updatedStop.latitude))
            .andExpect(jsonPath("$.longitude").value(updatedStop.longitude))
            .andExpect(jsonPath("$.mobileNumber").value(updatedStop.mobileNumber))
    }

    @Test
    @DisplayName("버스 정류장 수정 - 존재하지 않는 정류장")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBusStopNotFound() {
        val payload =
            UpdateBusStopRequest(
                name = "Updated Stop Name",
                latitude = 37.123456,
                longitude = 127.123456,
                mobileNumber = "031-123-4567",
                districtCode = 210000123,
                regionName = "Updated Region",
            )
        doThrow(BusStopNotFoundException()).whenever(stopService).updateBusStop(999999999, payload)
        mockMvc
            .perform(
                put("/api/v1/bus/stop/999999999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("BUS_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 정류장 수정 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBusStopOtherError() {
        val payload =
            UpdateBusStopRequest(
                name = "Updated Stop Name",
                latitude = 37.123456,
                longitude = 127.123456,
                mobileNumber = "031-123-4567",
                districtCode = 210000123,
                regionName = "Updated Region",
            )
        doThrow(RuntimeException()).whenever(stopService).updateBusStop(999999999, payload)
        mockMvc
            .perform(
                put("/api/v1/bus/stop/999999999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("버스 정류장 삭제")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteBusStop() {
        mockMvc
            .perform(delete("/api/v1/bus/stop/${TEST_STOP_1.id}"))
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("버스 정류장 삭제 - 존재하지 않는 정류장")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteBusStopNotFound() {
        doThrow(BusStopNotFoundException()).whenever(stopService).deleteBusStopById(999999999)
        mockMvc
            .perform(delete("/api/v1/bus/stop/999999999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("BUS_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 정류장 삭제 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteBusStopOtherError() {
        doThrow(RuntimeException()).whenever(stopService).deleteBusStopById(999999999)
        mockMvc
            .perform(delete("/api/v1/bus/stop/999999999"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("버스 시간표 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetBusTimetables() {
        whenever(
            timetableService.getBusTimetableList(
                routeID = null,
                startStopID = null,
                weekdays = null,
            ),
        ).thenReturn(
            listOf(
                BusTimetable(
                    seq = 1,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("05:30:00"),
                ),
                BusTimetable(
                    seq = 2,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("06:00:00"),
                ),
            ),
        )
        mockMvc
            .perform(get("/api/v1/bus/timetable"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.result.length()").value(2))
            .andExpect(jsonPath("$.result[0].seq").value(1))
            .andExpect(jsonPath("$.result[0].routeID").value(216000068))
            .andExpect(jsonPath("$.result[0].startStopID").value(216000358))
            .andExpect(jsonPath("$.result[0].dayType").value("weekdays"))
            .andExpect(jsonPath("$.result[0].departureTime").value("05:30:00"))
            .andExpect(jsonPath("$.result[1].seq").value(2))
            .andExpect(jsonPath("$.result[1].routeID").value(216000068))
            .andExpect(jsonPath("$.result[1].startStopID").value(216000358))
            .andExpect(jsonPath("$.result[1].dayType").value("weekdays"))
            .andExpect(jsonPath("$.result[1].departureTime").value("06:00:00"))
    }

    @Test
    @DisplayName("버스 시간표 목록 조회 - 노선 ID 필터링")
    @WithCustomMockUser(username = "test_user")
    fun testGetBusTimetablesFilterByRouteID() {
        whenever(
            timetableService.getBusTimetableList(
                routeID = TEST_ROUTE_1.id,
                startStopID = null,
                weekdays = null,
            ),
        ).thenReturn(
            listOf(
                BusTimetable(
                    seq = 1,
                    routeID = TEST_ROUTE_1.id,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("05:30:00"),
                ),
            ),
        )
        mockMvc
            .perform(get("/api/v1/bus/timetable").param("routeID", TEST_ROUTE_1.id.toString()))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.result.length()").value(1))
            .andExpect(jsonPath("$.result[0].seq").value(1))
            .andExpect(jsonPath("$.result[0].routeID").value(TEST_ROUTE_1.id))
            .andExpect(jsonPath("$.result[0].startStopID").value(216000358))
            .andExpect(jsonPath("$.result[0].dayType").value("weekdays"))
            .andExpect(jsonPath("$.result[0].departureTime").value("05:30:00"))
    }

    @Test
    @DisplayName("버스 시간표 목록 조회 - 시점 정류장 ID 필터링")
    @WithCustomMockUser(username = "test_user")
    fun testGetBusTimetablesFilterByStartStopID() {
        whenever(
            timetableService.getBusTimetableList(
                routeID = null,
                startStopID = TEST_STOP_1.id,
                weekdays = null,
            ),
        ).thenReturn(
            listOf(
                BusTimetable(
                    seq = 1,
                    routeID = 216000068,
                    startStopID = TEST_STOP_1.id,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("05:30:00"),
                ),
            ),
        )
        mockMvc
            .perform(get("/api/v1/bus/timetable").param("startStopID", TEST_STOP_1.id.toString()))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.result.length()").value(1))
            .andExpect(jsonPath("$.result[0].seq").value(1))
            .andExpect(jsonPath("$.result[0].routeID").value(216000068))
            .andExpect(jsonPath("$.result[0].startStopID").value(TEST_STOP_1.id))
            .andExpect(jsonPath("$.result[0].dayType").value("weekdays"))
            .andExpect(jsonPath("$.result[0].departureTime").value("05:30:00"))
    }

    @Test
    @DisplayName("버스 시간표 목록 조회 - 노선 ID 및 시점 정류장 ID 필터링")
    @WithCustomMockUser(username = "test_user")
    fun testGetBusTimetablesFilterByRouteIDAndStartStopID() {
        whenever(
            timetableService.getBusTimetableList(
                routeID = TEST_ROUTE_1.id,
                startStopID = TEST_STOP_1.id,
                weekdays = null,
            ),
        ).thenReturn(
            listOf(
                BusTimetable(
                    seq = 1,
                    routeID = TEST_ROUTE_1.id,
                    startStopID = TEST_STOP_1.id,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("05:30:00"),
                ),
            ),
        )
        mockMvc
            .perform(
                get("/api/v1/bus/timetable")
                    .param("routeID", TEST_ROUTE_1.id.toString())
                    .param("startStopID", TEST_STOP_1.id.toString()),
            ).andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.result.length()").value(1))
            .andExpect(jsonPath("$.result[0].seq").value(1))
            .andExpect(jsonPath("$.result[0].routeID").value(TEST_ROUTE_1.id))
            .andExpect(jsonPath("$.result[0].startStopID").value(TEST_STOP_1.id))
            .andExpect(jsonPath("$.result[0].dayType").value("weekdays"))
            .andExpect(jsonPath("$.result[0].departureTime").value("05:30:00"))
    }

    @Test
    @DisplayName("버스 시간표 항목 생성")
    @WithCustomMockUser(username = "test_user")
    fun testCreateBusTimetable() {
        val payload =
            BusTimetableRequest(
                routeID = TEST_ROUTE_1.id,
                startStopID = TEST_STOP_1.id,
                dayType = "weekdays",
                departureTime = "07:30:00",
            )
        val newTimetable =
            BusTimetable(
                seq = 3,
                routeID = payload.routeID,
                startStopID = payload.startStopID,
                weekday = payload.dayType,
                departureTime = LocalTime.parse("07:30:00"),
            )
        whenever(timetableService.createBusTimetable(payload)).thenReturn(newTimetable)
        mockMvc
            .perform(
                post("/api/v1/bus/timetable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.seq").value(newTimetable.seq))
            .andExpect(jsonPath("$.routeID").value(newTimetable.routeID))
            .andExpect(jsonPath("$.startStopID").value(newTimetable.startStopID))
            .andExpect(jsonPath("$.dayType").value(newTimetable.weekday))
            .andExpect(jsonPath("$.departureTime").value("07:30:00"))
    }

    @Test
    @DisplayName("버스 시간표 항목 생성 - 존재하지 않는 노선")
    @WithCustomMockUser(username = "test_user")
    fun testCreateBusTimetableRouteNotFound() {
        val payload =
            BusTimetableRequest(
                routeID = 999999999,
                startStopID = TEST_STOP_1.id,
                dayType = "weekdays",
                departureTime = "07:30:00",
            )
        doThrow(BusRouteNotFoundException()).whenever(timetableService).createBusTimetable(payload)
        mockMvc
            .perform(
                post("/api/v1/bus/timetable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("BUS_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 시간표 항목 생성 - 존재하지 않는 시점 정류장")
    @WithCustomMockUser(username = "test_user")
    fun testCreateBusTimetableStartStopNotFound() {
        val payload =
            BusTimetableRequest(
                routeID = TEST_ROUTE_1.id,
                startStopID = 999999999,
                dayType = "weekdays",
                departureTime = "07:30:00",
            )
        doThrow(BusStartStopNotFoundException()).whenever(timetableService).createBusTimetable(payload)
        mockMvc
            .perform(
                post("/api/v1/bus/timetable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("BUS_START_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 시간표 항목 생성 - 유효하지 않은 시간 형식")
    @WithCustomMockUser(username = "test_user")
    fun testCreateBusTimetableInvalidTimeFormat() {
        val payload =
            BusTimetableRequest(
                routeID = TEST_ROUTE_1.id,
                startStopID = TEST_STOP_1.id,
                dayType = "weekdays",
                departureTime = "invalid-time-format",
            )
        doThrow(LocalTimeNotValidException()).whenever(timetableService).createBusTimetable(payload)
        mockMvc
            .perform(
                post("/api/v1/bus/timetable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("INVALID_TIME_FORMAT"))
    }

    @Test
    @DisplayName("버스 시간표 항목 생성 - 중복된 시간표")
    @WithCustomMockUser(username = "test_user")
    fun testCreateBusTimetableDuplicateTimetable() {
        val payload =
            BusTimetableRequest(
                routeID = TEST_ROUTE_1.id,
                startStopID = TEST_STOP_1.id,
                dayType = "weekdays",
                departureTime = "07:30:00",
            )
        doThrow(DuplicateBusTimetableException()).whenever(timetableService).createBusTimetable(payload)
        mockMvc
            .perform(
                post("/api/v1/bus/timetable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("DUPLICATE_BUS_TIMETABLE"))
    }

    @Test
    @DisplayName("버스 시간표 항목 생성 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testCreateBusTimetableOtherError() {
        val payload =
            BusTimetableRequest(
                routeID = TEST_ROUTE_1.id,
                startStopID = TEST_STOP_1.id,
                dayType = "weekdays",
                departureTime = "07:30:00",
            )
        doThrow(RuntimeException()).whenever(timetableService).createBusTimetable(payload)
        mockMvc
            .perform(
                post("/api/v1/bus/timetable")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("버스 시간표 항목 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetBusTimetable() {
        val timetable =
            BusTimetable(
                seq = 1,
                routeID = 216000068,
                startStopID = 216000358,
                weekday = "weekdays",
                departureTime = LocalTime.parse("05:30:00"),
            )
        whenever(timetableService.getBusTimetableById(1)).thenReturn(timetable)
        mockMvc
            .perform(get("/api/v1/bus/timetable/1"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.seq").value(timetable.seq))
            .andExpect(jsonPath("$.routeID").value(timetable.routeID))
            .andExpect(jsonPath("$.startStopID").value(timetable.startStopID))
            .andExpect(jsonPath("$.dayType").value(timetable.weekday))
            .andExpect(jsonPath("$.departureTime").value("05:30:00"))
    }

    @Test
    @DisplayName("버스 시간표 항목 조회 - 존재하지 않는 시간표")
    @WithCustomMockUser(username = "test_user")
    fun testGetBusTimetableNotFound() {
        doThrow(BusTimetableNotFoundException()).whenever(timetableService).getBusTimetableById(999999999)
        mockMvc
            .perform(get("/api/v1/bus/timetable/999999999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("BUS_TIMETABLE_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 시간표 항목 조회 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testGetBusTimetableOtherError() {
        doThrow(RuntimeException()).whenever(timetableService).getBusTimetableById(999999999)
        mockMvc
            .perform(get("/api/v1/bus/timetable/999999999"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("버스 시간표 항목 수정")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBusTimetable() {
        val payload =
            BusTimetableRequest(
                routeID = TEST_ROUTE_1.id,
                startStopID = TEST_STOP_1.id,
                dayType = "weekends",
                departureTime = "08:00:00",
            )
        val updatedTimetable =
            BusTimetable(
                seq = 1,
                routeID = payload.routeID,
                startStopID = payload.startStopID,
                weekday = payload.dayType,
                departureTime = LocalTime.parse("08:00:00"),
            )
        whenever(timetableService.updateBusTimetable(1, payload)).thenReturn(updatedTimetable)
        mockMvc
            .perform(
                put("/api/v1/bus/timetable/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.seq").value(updatedTimetable.seq))
            .andExpect(jsonPath("$.routeID").value(updatedTimetable.routeID))
            .andExpect(jsonPath("$.startStopID").value(updatedTimetable.startStopID))
            .andExpect(jsonPath("$.dayType").value(updatedTimetable.weekday))
            .andExpect(jsonPath("$.departureTime").value("08:00:00"))
    }

    @Test
    @DisplayName("버스 시간표 항목 수정 - 존재하지 않는 시간표")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBusTimetableNotFound() {
        val payload =
            BusTimetableRequest(
                routeID = TEST_ROUTE_1.id,
                startStopID = TEST_STOP_1.id,
                dayType = "weekends",
                departureTime = "08:00:00",
            )
        doThrow(BusTimetableNotFoundException()).whenever(timetableService).updateBusTimetable(999999999, payload)
        mockMvc
            .perform(
                put("/api/v1/bus/timetable/999999999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("BUS_TIMETABLE_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 시간표 항목 수정 - 존재하지 않는 노선")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBusTimetableRouteNotFound() {
        val payload =
            BusTimetableRequest(
                routeID = 999999999,
                startStopID = TEST_STOP_1.id,
                dayType = "weekends",
                departureTime = "08:00:00",
            )
        doThrow(BusRouteNotFoundException()).whenever(timetableService).updateBusTimetable(1, payload)
        mockMvc
            .perform(
                put("/api/v1/bus/timetable/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("BUS_ROUTE_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 시간표 항목 수정 - 존재하지 않는 시점 정류장")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBusTimetableStartStopNotFound() {
        val payload =
            BusTimetableRequest(
                routeID = TEST_ROUTE_1.id,
                startStopID = 999999999,
                dayType = "weekends",
                departureTime = "08:00:00",
            )
        doThrow(BusStartStopNotFoundException()).whenever(timetableService).updateBusTimetable(1, payload)
        mockMvc
            .perform(
                put("/api/v1/bus/timetable/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("BUS_START_STOP_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 시간표 항목 수정 - 유효하지 않은 시간 형식")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBusTimetableInvalidTimeFormat() {
        val payload =
            BusTimetableRequest(
                routeID = TEST_ROUTE_1.id,
                startStopID = TEST_STOP_1.id,
                dayType = "weekends",
                departureTime = "invalid-time-format",
            )
        doThrow(LocalTimeNotValidException()).whenever(timetableService).updateBusTimetable(1, payload)
        mockMvc
            .perform(
                put("/api/v1/bus/timetable/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("INVALID_TIME_FORMAT"))
    }

    @Test
    @DisplayName("버스 시간표 항목 수정 - 중복된 시간표")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBusTimetableDuplicateTimetable() {
        val payload =
            BusTimetableRequest(
                routeID = TEST_ROUTE_1.id,
                startStopID = TEST_STOP_1.id,
                dayType = "weekends",
                departureTime = "08:00:00",
            )
        doThrow(DuplicateBusTimetableException()).whenever(timetableService).updateBusTimetable(1, payload)
        mockMvc
            .perform(
                put("/api/v1/bus/timetable/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("DUPLICATE_BUS_TIMETABLE"))
    }

    @Test
    @DisplayName("버스 시간표 항목 수정 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testUpdateBusTimetableOtherError() {
        val payload =
            BusTimetableRequest(
                routeID = TEST_ROUTE_1.id,
                startStopID = TEST_STOP_1.id,
                dayType = "weekends",
                departureTime = "08:00:00",
            )
        doThrow(RuntimeException()).whenever(timetableService).updateBusTimetable(1, payload)
        mockMvc
            .perform(
                put("/api/v1/bus/timetable/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("버스 시간표 항목 삭제")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteBusTimetable() {
        mockMvc
            .perform(delete("/api/v1/bus/timetable/1"))
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("버스 시간표 항목 삭제 - 존재하지 않는 시간표")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteBusTimetableNotFound() {
        doThrow(BusTimetableNotFoundException()).whenever(timetableService).deleteBusTimetableById(999999999)
        mockMvc
            .perform(delete("/api/v1/bus/timetable/999999999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("BUS_TIMETABLE_NOT_FOUND"))
    }

    @Test
    @DisplayName("버스 시간표 항목 삭제 - 기타 오류")
    @WithCustomMockUser(username = "test_user")
    fun testDeleteBusTimetableOtherError() {
        doThrow(RuntimeException()).whenever(timetableService).deleteBusTimetableById(999999999)
        mockMvc
            .perform(delete("/api/v1/bus/timetable/999999999"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("버스 실시간 도착 목록 조회")
    @WithCustomMockUser(username = "test_user")
    fun testGetRealTimeArrivals() {
        whenever(realtimeService.getBusRealtimeList()).thenReturn(
            listOf(
                BusRealtime(
                    routeID = 216000068,
                    stopID = 216000138,
                    order = 1,
                    remainingTime = Duration.ofMinutes(5),
                    remainingStop = 2,
                    remainingSeat = 40,
                    isLowFloor = true,
                    updatedAt = ZonedDateTime.now(),
                    routeStop = null,
                ),
                BusRealtime(
                    routeID = 216000068,
                    stopID = 216000139,
                    order = 2,
                    remainingTime = Duration.ofMinutes(15),
                    remainingStop = 5,
                    remainingSeat = 20,
                    isLowFloor = false,
                    updatedAt = ZonedDateTime.now(),
                    routeStop = null,
                ),
            ),
        )
        mockMvc
            .perform(get("/api/v1/bus/realtime"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.result.length()").value(2))
            .andExpect(jsonPath("$.result[0].routeID").value(216000068))
            .andExpect(jsonPath("$.result[0].stopID").value(216000138))
            .andExpect(jsonPath("$.result[0].order").value(1))
            .andExpect(jsonPath("$.result[0].time").value("00:05:00"))
            .andExpect(jsonPath("$.result[0].stop").value(2))
            .andExpect(jsonPath("$.result[0].seat").value(40))
            .andExpect(jsonPath("$.result[0].isLowFloor").value(true))
            .andExpect(jsonPath("$.result[1].routeID").value(216000068))
            .andExpect(jsonPath("$.result[1].stopID").value(216000139))
            .andExpect(jsonPath("$.result[1].order").value(2))
            .andExpect(jsonPath("$.result[1].time").value("00:15:00"))
            .andExpect(jsonPath("$.result[1].stop").value(5))
            .andExpect(jsonPath("$.result[1].seat").value(20))
            .andExpect(jsonPath("$.result[1].isLowFloor").value(false))
    }
}
