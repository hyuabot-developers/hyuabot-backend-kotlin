package app.hyuabot.backend.bus

import app.hyuabot.backend.bus.domain.BusArrivalKey
import app.hyuabot.backend.bus.domain.BusDepartureLogKey
import app.hyuabot.backend.bus.domain.BusRouteStopRequest
import app.hyuabot.backend.bus.domain.BusTimetableKey
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
import app.hyuabot.backend.codegen.types.BusRouteStopInput
import app.hyuabot.backend.database.entity.BusDepartureLog
import app.hyuabot.backend.database.entity.BusRealtime
import app.hyuabot.backend.database.entity.BusRoute
import app.hyuabot.backend.database.entity.BusRouteStop
import app.hyuabot.backend.database.entity.BusStop
import app.hyuabot.backend.database.entity.BusTimetable
import app.hyuabot.backend.database.exception.LocalTimeNotValidException
import app.hyuabot.backend.database.repository.BusDepartureLogRepository
import app.hyuabot.backend.database.repository.BusRealtimeRepository
import app.hyuabot.backend.database.repository.BusRouteRepository
import app.hyuabot.backend.database.repository.BusRouteStopRepository
import app.hyuabot.backend.database.repository.BusStopRepository
import app.hyuabot.backend.database.repository.BusTimetableRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.spy
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Sort
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.Optional
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class BusServiceTest {
    @Mock
    private lateinit var routeRepository: BusRouteRepository

    @Mock
    private lateinit var stopRepository: BusStopRepository

    @Mock
    private lateinit var routeStopRepository: BusRouteStopRepository

    @Mock
    private lateinit var realtimeRepository: BusRealtimeRepository

    @Mock
    private lateinit var timetableRepository: BusTimetableRepository

    @Mock
    private lateinit var logRepository: BusDepartureLogRepository

    @InjectMocks
    private lateinit var routeService: BusRouteService

    @InjectMocks
    private lateinit var stopService: BusStopService

    @InjectMocks
    private lateinit var timetableService: BusTimetableService

    @InjectMocks
    private lateinit var realtimeService: BusRealtimeService

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
        val TEST_ROUTE_2 =
            BusRoute(
                id = 216000069,
                name = "10-2",
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
    fun testGetBusRouteList() {
        whenever(routeRepository.findAll()).thenReturn(listOf(TEST_ROUTE_1, TEST_ROUTE_2))
        val routes = routeService.getBusRouteList()
        assertEquals(2, routes.size)
        assertEquals("10-1", routes[0].name)
        assertEquals("10-2", routes[1].name)
    }

    @Test
    @DisplayName("버스 노선 생성")
    fun testCreateBusRoute() {
        val payload =
            CreateBusRouteRequest(
                id = 216000070,
                name = "10-3",
                typeCode = "13",
                typeName = "일반형시내버스",
                startStopID = 216000358,
                endStopID = 216000138,
                upFirstTime = "05:30:00",
                upLastTime = "23:10:00",
                downFirstTime = "06:00:00",
                downLastTime = "23:40:00",
                districtCode = 2,
                companyID = 4100700,
                companyName = "경원여객",
                companyPhone = "031-492-2260",
            )
        whenever(routeRepository.findById(216000070)).thenReturn(Optional.empty())
        whenever(stopRepository.findById(216000358)).thenReturn(Optional.of(TEST_STOP_1))
        whenever(stopRepository.findById(216000138)).thenReturn(Optional.of(TEST_STOP_2))
        whenever(
            routeRepository.save(
                BusRoute(
                    id = 216000070,
                    name = "10-3",
                    typeCode = "13",
                    typeName = "일반형시내버스",
                    startStopID = 216000358,
                    endStopID = 216000138,
                    upFirstTime = LocalTime.parse("05:30:00"),
                    upLastTime = LocalTime.parse("23:10:00"),
                    downFirstTime = LocalTime.parse("06:00:00"),
                    downLastTime = LocalTime.parse("23:40:00"),
                    districtCode = 2,
                    companyID = 4100700,
                    companyName = "경원여객",
                    companyPhone = "031-492-2260",
                    stop = emptyList(),
                ),
            ),
        ).thenReturn(
            BusRoute(
                id = 216000070,
                name = "10-3",
                typeCode = "13",
                typeName = "일반형시내버스",
                startStopID = 216000358,
                endStopID = 216000138,
                upFirstTime = LocalTime.parse("05:30:00"),
                upLastTime = LocalTime.parse("23:10:00"),
                downFirstTime = LocalTime.parse("06:00:00"),
                downLastTime = LocalTime.parse("23:40:00"),
                districtCode = 2,
                companyID = 4100700,
                companyName = "경원여객",
                companyPhone = "031-492-2260",
                stop = emptyList(),
            ),
        )
        val route = routeService.createBusRoute(payload)
        assertEquals(216000070, route.id)
        assertEquals("10-3", route.name)
        assertEquals("13", route.typeCode)
        assertEquals("일반형시내버스", route.typeName)
        assertEquals(216000358, route.startStopID)
        assertEquals(216000138, route.endStopID)
    }

    @Test
    @DisplayName("버스 노선 생성 - 상행 첫차 시간 형식 오류")
    fun testCreateBusRouteInvalidUpFirstTime() {
        val payload =
            CreateBusRouteRequest(
                id = 216000070,
                name = "10-3",
                typeCode = "13",
                typeName = "일반형시내버스",
                startStopID = 216000358,
                endStopID = 216000138,
                upFirstTime = "05:30",
                upLastTime = "23:10:00",
                downFirstTime = "06:00:00",
                downLastTime = "23:40:00",
                districtCode = 2,
                companyID = 4100700,
                companyName = "경원여객",
                companyPhone = "031-492-2260",
            )
        assertThrows<LocalTimeNotValidException> {
            routeService.createBusRoute(payload)
        }
    }

    @Test
    @DisplayName("버스 노선 생성 - 상행 막차 시간 형식 오류")
    fun testCreateBusRouteInvalidUpLastTime() {
        val payload =
            CreateBusRouteRequest(
                id = 216000070,
                name = "10-3",
                typeCode = "13",
                typeName = "일반형시내버스",
                startStopID = 216000358,
                endStopID = 216000138,
                upFirstTime = "05:30:00",
                upLastTime = "23:10",
                downFirstTime = "06:00:00",
                downLastTime = "23:40:00",
                districtCode = 2,
                companyID = 4100700,
                companyName = "경원여객",
                companyPhone = "031-492-2260",
            )
        assertThrows<LocalTimeNotValidException> {
            routeService.createBusRoute(payload)
        }
    }

    @Test
    @DisplayName("버스 노선 생성 - 하행 첫차 시간 형식 오류")
    fun testCreateBusRouteInvalidDownFirstTime() {
        val payload =
            CreateBusRouteRequest(
                id = 216000070,
                name = "10-3",
                typeCode = "13",
                typeName = "일반형시내버스",
                startStopID = 216000358,
                endStopID = 216000138,
                upFirstTime = "05:30:00",
                upLastTime = "23:10:00",
                downFirstTime = "06:00",
                downLastTime = "23:40:00",
                districtCode = 2,
                companyID = 4100700,
                companyName = "경원여객",
                companyPhone = "031-492-2260",
            )
        assertThrows<LocalTimeNotValidException> {
            routeService.createBusRoute(payload)
        }
    }

    @Test
    @DisplayName("버스 노선 생성 - 하행 막차 시간 형식 오류")
    fun testCreateBusRouteInvalidDownLastTime() {
        val payload =
            CreateBusRouteRequest(
                id = 216000070,
                name = "10-3",
                typeCode = "13",
                typeName = "일반형시내버스",
                startStopID = 216000358,
                endStopID = 216000138,
                upFirstTime = "05:30:00",
                upLastTime = "23:10:00",
                downFirstTime = "06:00:00",
                downLastTime = "23:40",
                districtCode = 2,
                companyID = 4100700,
                companyName = "경원여객",
                companyPhone = "031-492-2260",
            )
        assertThrows<LocalTimeNotValidException> {
            routeService.createBusRoute(payload)
        }
    }

    @Test
    @DisplayName("버스 노선 생성 - 중복된 ID")
    fun testCreateBusRouteDuplicateID() {
        val payload =
            CreateBusRouteRequest(
                id = 216000068,
                name = "10-1",
                typeCode = "13",
                typeName = "일반형시내버스",
                startStopID = 216000358,
                endStopID = 216000138,
                upFirstTime = "05:30:00",
                upLastTime = "23:10:00",
                downFirstTime = "06:00:00",
                downLastTime = "23:40:00",
                districtCode = 2,
                companyID = 4100700,
                companyName = "경원여객",
                companyPhone = "031-492-2260",
            )
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        assertThrows<DuplicateBusRouteException> {
            routeService.createBusRoute(payload)
        }
    }

    @Test
    @DisplayName("버스 노선 생성 - 존재하지 않는 기점 정류장")
    fun testCreateBusRouteNonExistentStartStop() {
        val payload =
            CreateBusRouteRequest(
                id = 216000070,
                name = "10-3",
                typeCode = "13",
                typeName = "일반형시내버스",
                startStopID = 216000358,
                endStopID = 216000138,
                upFirstTime = "05:30:00",
                upLastTime = "23:10:00",
                downFirstTime = "06:00:00",
                downLastTime = "23:40:00",
                districtCode = 2,
                companyID = 4100700,
                companyName = "경원여객",
                companyPhone = "031-492-2260",
            )
        whenever(routeRepository.findById(216000070)).thenReturn(Optional.empty())
        whenever(stopRepository.findById(216000358)).thenReturn(Optional.empty())
        assertThrows<BusStartStopNotFoundException> {
            routeService.createBusRoute(payload)
        }
    }

    @Test
    @DisplayName("버스 노선 생성 - 존재하지 않는 종점 정류장")
    fun testCreateBusRouteNonExistentEndStop() {
        val payload =
            CreateBusRouteRequest(
                id = 216000070,
                name = "10-3",
                typeCode = "13",
                typeName = "일반형시내버스",
                startStopID = 216000358,
                endStopID = 216000138,
                upFirstTime = "05:30:00",
                upLastTime = "23:10:00",
                downFirstTime = "06:00:00",
                downLastTime = "23:40:00",
                districtCode = 2,
                companyID = 4100700,
                companyName = "경원여객",
                companyPhone = "031-492-2260",
            )
        whenever(routeRepository.findById(216000070)).thenReturn(Optional.empty())
        whenever(stopRepository.findById(216000358)).thenReturn(Optional.of(TEST_STOP_1))
        whenever(stopRepository.findById(216000138)).thenReturn(Optional.empty())
        assertThrows<BusEndStopNotFoundException> {
            routeService.createBusRoute(payload)
        }
    }

    @Test
    @DisplayName("버스 노선 항목 조회")
    fun testGetBusRouteById() {
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        val route = routeService.getBusRouteById(216000068)
        assertEquals(216000068, route.id)
        assertEquals("10-1", route.name)
        assertEquals("13", route.typeCode)
        assertEquals("일반형시내버스", route.typeName)
        assertEquals(216000358, route.startStopID)
        assertEquals(216000138, route.endStopID)
    }

    @Test
    @DisplayName("버스 노선 항목 조회 - 존재하지 않는 ID")
    fun testGetBusRouteByIdNonExistentID() {
        whenever(routeRepository.findById(216000999)).thenReturn(Optional.empty())
        assertThrows<BusRouteNotFoundException> {
            routeService.getBusRouteById(216000999)
        }
    }

    @Test
    @DisplayName("버스 노선 수정")
    fun testUpdateBusRoute() {
        val payload =
            UpdateBusRouteRequest(
                name = "10-1-1",
                typeCode = "13",
                typeName = "일반형시내버스",
                startStopID = 216000358,
                endStopID = 216000138,
                upFirstTime = "05:30:00",
                upLastTime = "23:10:00",
                downFirstTime = "06:00:00",
                downLastTime = "23:40:00",
                districtCode = 2,
                companyID = 4100700,
                companyName = "경원여객",
                companyPhone = "031-492-2260",
            )
        val updatedRoute = TEST_ROUTE_1.copy(name = "10-1-1")
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        whenever(stopRepository.findById(216000358)).thenReturn(Optional.of(TEST_STOP_1))
        whenever(stopRepository.findById(216000138)).thenReturn(Optional.of(TEST_STOP_2))
        whenever(routeRepository.save(updatedRoute)).thenReturn(updatedRoute)
        val result = routeService.updateBusRoute(216000068, payload)
        assertEquals(216000068, result.id)
        assertEquals("10-1-1", result.name)
    }

    @Test
    @DisplayName("버스 노선 수정 - 존재하지 않는 ID")
    fun testUpdateBusRouteNonExistentID() {
        val payload =
            UpdateBusRouteRequest(
                name = "10-9",
                typeCode = "13",
                typeName = "일반형시내버스",
                startStopID = 216000358,
                endStopID = 216000138,
                upFirstTime = "05:30:00",
                upLastTime = "23:10:00",
                downFirstTime = "06:00:00",
                downLastTime = "23:40:00",
                districtCode = 2,
                companyID = 4100700,
                companyName = "경원여객",
                companyPhone = "031-492-2260",
            )
        whenever(routeRepository.findById(216000999)).thenReturn(Optional.empty())
        assertThrows<BusRouteNotFoundException> {
            routeService.updateBusRoute(216000999, payload)
        }
    }

    @Test
    @DisplayName("버스 노선 수정 - 상행 첫차 시간 형식 오류")
    fun testUpdateBusRouteInvalidUpFirstTime() {
        val payload =
            UpdateBusRouteRequest(
                name = "10-1-1",
                typeCode = "13",
                typeName = "일반형시내버스",
                startStopID = 216000358,
                endStopID = 216000138,
                upFirstTime = "05:30",
                upLastTime = "23:10:00",
                downFirstTime = "06:00:00",
                downLastTime = "23:40:00",
                districtCode = 2,
                companyID = 4100700,
                companyName = "경원여객",
                companyPhone = "031-492-2260",
            )
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        assertThrows<LocalTimeNotValidException> {
            routeService.updateBusRoute(216000068, payload)
        }
    }

    @Test
    @DisplayName("버스 노선 수정 - 상행 막차 시간 형식 오류")
    fun testUpdateBusRouteInvalidUpLastTime() {
        val payload =
            UpdateBusRouteRequest(
                name = "10-1-1",
                typeCode = "13",
                typeName = "일반형시내버스",
                startStopID = 216000358,
                endStopID = 216000138,
                upFirstTime = "05:30:00",
                upLastTime = "23:10",
                downFirstTime = "06:00:00",
                downLastTime = "23:40:00",
                districtCode = 2,
                companyID = 4100700,
                companyName = "경원여객",
                companyPhone = "031-492-2260",
            )
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        assertThrows<LocalTimeNotValidException> {
            routeService.updateBusRoute(216000068, payload)
        }
    }

    @Test
    @DisplayName("버스 노선 수정 - 하행 첫차 시간 형식 오류")
    fun testUpdateBusRouteInvalidDownFirstTime() {
        val payload =
            UpdateBusRouteRequest(
                name = "10-1-1",
                typeCode = "13",
                typeName = "일반형시내버스",
                startStopID = 216000358,
                endStopID = 216000138,
                upFirstTime = "05:30:00",
                upLastTime = "23:10:00",
                downFirstTime = "06:00",
                downLastTime = "23:40:00",
                districtCode = 2,
                companyID = 4100700,
                companyName = "경원여객",
                companyPhone = "031-492-2260",
            )
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        assertThrows<LocalTimeNotValidException> {
            routeService.updateBusRoute(216000068, payload)
        }
    }

    @Test
    @DisplayName("버스 노선 수정 - 하행 막차 시간 형식 오류")
    fun testUpdateBusRouteInvalidDownLastTime() {
        val payload =
            UpdateBusRouteRequest(
                name = "10-1-1",
                typeCode = "13",
                typeName = "일반형시내버스",
                startStopID = 216000358,
                endStopID = 216000138,
                upFirstTime = "05:30:00",
                upLastTime = "23:10:00",
                downFirstTime = "06:00:00",
                downLastTime = "23:40",
                districtCode = 2,
                companyID = 4100700,
                companyName = "경원여객",
                companyPhone = "031-492-2260",
            )
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        assertThrows<LocalTimeNotValidException> {
            routeService.updateBusRoute(216000068, payload)
        }
    }

    @Test
    @DisplayName("버스 노선 수정 - 존재하지 않는 기점 정류장")
    fun testUpdateBusRouteNonExistentStartStop() {
        val payload =
            UpdateBusRouteRequest(
                name = "10-1-1",
                typeCode = "13",
                typeName = "일반형시내버스",
                startStopID = 216000358,
                endStopID = 216000138,
                upFirstTime = "05:30:00",
                upLastTime = "23:10:00",
                downFirstTime = "06:00:00",
                downLastTime = "23:40:00",
                districtCode = 2,
                companyID = 4100700,
                companyName = "경원여객",
                companyPhone = "031-492-2260",
            )
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        whenever(stopRepository.findById(216000358)).thenReturn(Optional.empty())
        assertThrows<BusStartStopNotFoundException> {
            routeService.updateBusRoute(216000068, payload)
        }
    }

    @Test
    @DisplayName("버스 노선 수정 - 존재하지 않는 종점 정류장")
    fun testUpdateBusRouteNonExistentEndStop() {
        val payload =
            UpdateBusRouteRequest(
                name = "10-1-1",
                typeCode = "13",
                typeName = "일반형시내버스",
                startStopID = 216000358,
                endStopID = 216000138,
                upFirstTime = "05:30:00",
                upLastTime = "23:10:00",
                downFirstTime = "06:00:00",
                downLastTime = "23:40:00",
                districtCode = 2,
                companyID = 4100700,
                companyName = "경원여객",
                companyPhone = "031-492-2260",
            )
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        whenever(stopRepository.findById(216000358)).thenReturn(Optional.of(TEST_STOP_1))
        whenever(stopRepository.findById(216000138)).thenReturn(Optional.empty())
        assertThrows<BusEndStopNotFoundException> {
            routeService.updateBusRoute(216000068, payload)
        }
    }

    @Test
    @DisplayName("버스 노선 삭제")
    fun testDeleteBusRoute() {
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        routeService.deleteBusRouteById(216000068)
        verify(routeRepository).delete(TEST_ROUTE_1)
    }

    @Test
    @DisplayName("버스 노선 삭제 - 존재하지 않는 ID")
    fun testDeleteBusRouteNonExistentID() {
        whenever(routeRepository.findById(216000999)).thenReturn(Optional.empty())
        assertThrows<BusRouteNotFoundException> {
            routeService.deleteBusRouteById(216000999)
        }
    }

    @Test
    @DisplayName("버스 노선 정류장 목록 조회")
    fun testGetBusStopsByRouteId() {
        whenever(routeRepository.findById(216000068)).thenReturn(
            Optional.of(
                TEST_ROUTE_1.copy(
                    stop = listOf(TEST_ROUTE_STOP_1, TEST_ROUTE_STOP_2),
                ),
            ),
        )
        val result = routeService.getBusStopListByRouteID(216000068)
        assertEquals(2, result.size)
        assertEquals(216000358, result[0].stopID)
        assertEquals(216000138, result[1].stopID)
    }

    @Test
    @DisplayName("버스 노선 정류장 목록 조회 - 노선 ID / 정류장 ID 필터링")
    fun testGetBusStopsByRouteIdWithFilter() {
        val input =
            listOf(
                BusRouteStopInput(
                    route = TEST_ROUTE_1.id,
                    stop = TEST_ROUTE_STOP_1.stopID,
                    dates = listOf(),
                ),
                BusRouteStopInput(
                    route = TEST_ROUTE_1.id,
                    stop = TEST_ROUTE_STOP_2.stopID,
                    dates = listOf(),
                ),
            )
        whenever(
            routeStopRepository.fetchBusRouteStops(
                listOf(TEST_ROUTE_1.id),
                listOf(TEST_ROUTE_STOP_1.stopID, TEST_ROUTE_STOP_2.stopID),
            ),
        ).thenReturn(
            listOf(TEST_ROUTE_STOP_1, TEST_ROUTE_STOP_2),
        )
        val result =
            routeService.fetchRouteStops(input)
        assertEquals(
            true,
            result.all {
                BusRouteStopInput(
                    route = it.routeID,
                    stop = it.stopID,
                    dates = listOf(),
                ) in input
            },
        )
        val result2 =
            routeService.fetchRouteStops(emptyList())
        assertEquals(true, result2.isEmpty())
    }

    @Test
    @DisplayName("버스 노선 정류장 목록 조회 - 존재하지 않는 노선 ID")
    fun testGetBusStopsByRouteIdNonExistentID() {
        whenever(routeRepository.findById(216000999)).thenReturn(Optional.empty())
        assertThrows<BusRouteNotFoundException> {
            routeService.getBusStopListByRouteID(216000999)
        }
    }

    @Test
    @DisplayName("버스 노선 - 정류장 추가")
    fun testAddBusStopToRoute() {
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        whenever(stopRepository.findById(216000138)).thenReturn(Optional.of(TEST_STOP_2))
        whenever(stopRepository.findById(216000139)).thenReturn(
            Optional.of(
                BusStop(
                    id = 216000139,
                    name = "테스트정류장",
                    districtCode = 2,
                    latitude = 37.785000,
                    longitude = 127.051000,
                    mobileNumber = "00000",
                    regionName = "경기",
                    busRoutes = emptyList(),
                    startBusRoutes = emptyList(),
                ),
            ),
        )
        whenever(routeStopRepository.findByRouteIDAndOrder(216000068, 3)).thenReturn(null)
        whenever(
            routeStopRepository.save(
                BusRouteStop(
                    seq = null,
                    routeID = 216000068,
                    stopID = 216000139,
                    startStopID = 216000138,
                    order = 3,
                    minuteFromStart = 20,
                    route = null,
                    stop = null,
                    startStop = null,
                    log = emptyList(),
                    realtime = emptyList(),
                ),
            ),
        ).thenReturn(
            BusRouteStop(
                seq = 3,
                routeID = 216000068,
                stopID = 216000139,
                startStopID = 216000138,
                order = 3,
                minuteFromStart = 20,
                route = null,
                stop = null,
                startStop = null,
                log = emptyList(),
                realtime = emptyList(),
            ),
        )
        val result =
            routeService.createBusRouteStop(
                216000068,
                BusRouteStopRequest(
                    stopID = 216000139,
                    startStopID = 216000138,
                    order = 3,
                    travelTime = 20,
                ),
            )
        assertEquals(216000068, result.routeID)
        assertEquals(216000139, result.stopID)
        assertEquals(3, result.order)
        assertEquals(20, result.minuteFromStart)
    }

    @Test
    @DisplayName("버스 노선 - 정류장 추가 (존재하지 않는 노선 ID)")
    fun testAddBusStopToRouteNonExistentRouteID() {
        whenever(routeRepository.findById(216000999)).thenReturn(Optional.empty())
        assertThrows<BusRouteNotFoundException> {
            routeService.createBusRouteStop(
                216000999,
                BusRouteStopRequest(
                    stopID = 216000139,
                    startStopID = 216000138,
                    order = 3,
                    travelTime = 20,
                ),
            )
        }
    }

    @Test
    @DisplayName("버스 노선 - 정류장 추가 (존재하지 않는 정류장 ID)")
    fun testAddBusStopToRouteNonExistentStopID() {
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        whenever(stopRepository.findById(216000139)).thenReturn(Optional.empty())
        assertThrows<BusStopNotFoundException> {
            routeService.createBusRouteStop(
                216000068,
                BusRouteStopRequest(
                    stopID = 216000139,
                    startStopID = 216000138,
                    order = 3,
                    travelTime = 20,
                ),
            )
        }
    }

    @Test
    @DisplayName("버스 노선 - 정류장 추가 (존재하지 않는 기점 정류장 ID)")
    fun testAddBusStopToRouteNonExistentStartStopID() {
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        whenever(stopRepository.findById(216000139)).thenReturn(
            Optional.of(
                BusStop(
                    id = 216000139,
                    name = "테스트정류장",
                    districtCode = 2,
                    latitude = 37.785000,
                    longitude = 127.051000,
                    mobileNumber = "00000",
                    regionName = "경기",
                    busRoutes = emptyList(),
                    startBusRoutes = emptyList(),
                ),
            ),
        )
        whenever(stopRepository.findById(216000138)).thenReturn(Optional.empty())
        assertThrows<BusStartStopNotFoundException> {
            routeService.createBusRouteStop(
                216000068,
                BusRouteStopRequest(
                    stopID = 216000139,
                    startStopID = 216000138,
                    order = 3,
                    travelTime = 20,
                ),
            )
        }
    }

    @Test
    @DisplayName("버스 노선 - 정류장 추가 (중복된 순서)")
    fun testAddBusStopToRouteDuplicateOrder() {
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        whenever(stopRepository.findById(216000138)).thenReturn(Optional.of(TEST_STOP_2))
        whenever(stopRepository.findById(216000139)).thenReturn(
            Optional.of(
                BusStop(
                    id = 216000139,
                    name = "테스트정류장",
                    districtCode = 2,
                    latitude = 37.785000,
                    longitude = 127.051000,
                    mobileNumber = "00000",
                    regionName = "경기",
                    busRoutes = emptyList(),
                    startBusRoutes = emptyList(),
                ),
            ),
        )
        whenever(routeStopRepository.findByRouteIDAndOrder(216000068, 2)).thenReturn(TEST_ROUTE_STOP_2)
        assertThrows<DuplicateBusRouteStopException> {
            routeService.createBusRouteStop(
                216000068,
                BusRouteStopRequest(
                    stopID = 216000139,
                    startStopID = 216000138,
                    order = 2,
                    travelTime = 20,
                ),
            )
        }
    }

    @Test
    @DisplayName("버스 노선 - 정류장 수정")
    fun testUpdateBusStopInRoute() {
        val updatedRouteStop = TEST_ROUTE_STOP_2.copy(order = 3, minuteFromStart = 15)
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        whenever(routeStopRepository.findByRouteIDAndSeq(216000068, 2)).thenReturn(TEST_ROUTE_STOP_2)
        whenever(routeStopRepository.findByRouteIDAndOrderAndSeqNot(216000068, 3, 2)).thenReturn(null)
        whenever(stopRepository.findById(216000138)).thenReturn(Optional.of(TEST_STOP_2))
        whenever(stopRepository.findById(216000358)).thenReturn(Optional.of(TEST_STOP_1))
        whenever(
            routeStopRepository.save(
                TEST_ROUTE_STOP_2.copy(order = 3, minuteFromStart = 15),
            ),
        ).thenReturn(updatedRouteStop)
        val result =
            routeService.updateBusRouteStop(
                216000068,
                2,
                BusRouteStopRequest(
                    stopID = 216000138,
                    startStopID = 216000358,
                    order = 3,
                    travelTime = 15,
                ),
            )
        assertEquals(216000068, result.routeID)
        assertEquals(216000138, result.stopID)
        assertEquals(3, result.order)
        assertEquals(15, result.minuteFromStart)
    }

    @Test
    @DisplayName("버스 노선 - 정류장 수정 (존재하지 않는 노선 ID)")
    fun testUpdateBusStopInRouteNonExistentRouteID() {
        whenever(routeRepository.findById(216000999)).thenReturn(Optional.empty())
        assertThrows<BusRouteNotFoundException> {
            routeService.updateBusRouteStop(
                216000999,
                2,
                BusRouteStopRequest(
                    stopID = 216000138,
                    startStopID = 216000358,
                    order = 3,
                    travelTime = 15,
                ),
            )
        }
    }

    @Test
    @DisplayName("버스 노선 - 정류장 수정 (존재하지 않는 노선 정류장 Seq)")
    fun testUpdateBusStopInRouteNonExistentRouteStopSeq() {
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        whenever(routeStopRepository.findByRouteIDAndSeq(216000068, 99)).thenReturn(null)
        assertThrows<BusRouteStopNotFoundException> {
            routeService.updateBusRouteStop(
                216000068,
                99,
                BusRouteStopRequest(
                    stopID = 216000138,
                    startStopID = 216000358,
                    order = 3,
                    travelTime = 15,
                ),
            )
        }
    }

    @Test
    @DisplayName("버스 노선 - 정류장 수정 (존재하지 않는 정류장 ID)")
    fun testUpdateBusStopInRouteNonExistentStopID() {
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        whenever(routeStopRepository.findByRouteIDAndSeq(216000068, 2)).thenReturn(TEST_ROUTE_STOP_2)
        whenever(stopRepository.findById(216000138)).thenReturn(Optional.empty())
        assertThrows<BusStopNotFoundException> {
            routeService.updateBusRouteStop(
                216000068,
                2,
                BusRouteStopRequest(
                    stopID = 216000138,
                    startStopID = 216000358,
                    order = 3,
                    travelTime = 15,
                ),
            )
        }
    }

    @Test
    @DisplayName("버스 노선 - 정류장 수정 (존재하지 않는 기점 정류장 ID)")
    fun testUpdateBusStopInRouteNonExistentStartStop() {
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        whenever(routeStopRepository.findByRouteIDAndSeq(216000068, 2)).thenReturn(TEST_ROUTE_STOP_2)
        whenever(stopRepository.findById(216000138)).thenReturn(Optional.of(TEST_STOP_2))
        whenever(stopRepository.findById(216000358)).thenReturn(Optional.empty())
        assertThrows<BusStartStopNotFoundException> {
            routeService.updateBusRouteStop(
                216000068,
                2,
                BusRouteStopRequest(
                    stopID = 216000138,
                    startStopID = 216000358,
                    order = 3,
                    travelTime = 15,
                ),
            )
        }
    }

    @Test
    @DisplayName("버스 노선 - 정류장 수정 (중복된 순서)")
    fun testUpdateBusStopInRouteDuplicateOrder() {
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        whenever(routeStopRepository.findByRouteIDAndSeq(216000068, 2)).thenReturn(TEST_ROUTE_STOP_2)
        whenever(routeStopRepository.findByRouteIDAndOrderAndSeqNot(216000068, 1, 2)).thenReturn(TEST_ROUTE_STOP_1)
        whenever(stopRepository.findById(216000138)).thenReturn(Optional.of(TEST_STOP_2))
        whenever(stopRepository.findById(216000358)).thenReturn(Optional.of(TEST_STOP_1))
        assertThrows<DuplicateBusRouteStopException> {
            routeService.updateBusRouteStop(
                216000068,
                2,
                BusRouteStopRequest(
                    stopID = 216000138,
                    startStopID = 216000358,
                    order = 1,
                    travelTime = 15,
                ),
            )
        }
    }

    @Test
    @DisplayName("버스 노선 - 정류장 삭제")
    fun testDeleteBusStopFromRoute() {
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        whenever(routeStopRepository.findByRouteIDAndSeq(216000068, 2)).thenReturn(TEST_ROUTE_STOP_2)
        routeService.deleteBusRouteStopBySeq(216000068, 2)
        verify(routeStopRepository).delete(TEST_ROUTE_STOP_2)
    }

    @Test
    @DisplayName("버스 노선 - 정류장 삭제 (존재하지 않는 노선 ID)")
    fun testDeleteBusStopFromRouteNonExistentRouteID() {
        whenever(routeRepository.findById(216000999)).thenReturn(Optional.empty())
        assertThrows<BusRouteNotFoundException> {
            routeService.deleteBusRouteStopBySeq(216000999, 2)
        }
    }

    @Test
    @DisplayName("버스 노선 - 정류장 삭제 (존재하지 않는 노선 정류장 Seq)")
    fun testDeleteBusStopFromRouteNonExistentRouteStopSeq() {
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        whenever(routeStopRepository.findByRouteIDAndSeq(216000068, 99)).thenReturn(null)
        assertThrows<BusRouteStopNotFoundException> {
            routeService.deleteBusRouteStopBySeq(216000068, 99)
        }
    }

    @Test
    @DisplayName("버스 도착 로그 조회")
    fun testGetBusArrivalLogs() {
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        whenever(routeStopRepository.findByRouteIDAndSeq(216000068, 2)).thenReturn(TEST_ROUTE_STOP_2)
        whenever(logRepository.findByRouteIDAndStopID(216000068, 216000138)).thenReturn(
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
        )
        val result = routeService.getBusDepartureLogByRouteStop(216000068, 2)
        assertEquals(2, result.size)
        assertEquals("123가4567", result[0].vehicleID)
        assertEquals(LocalTime.parse("14:30:00"), result[0].departureTime)
        assertEquals("234나5678", result[1].vehicleID)
        assertEquals(LocalTime.parse("15:00:00"), result[1].departureTime)
    }

    @Test
    @DisplayName("버스 도착 로그 조회 - 노선 ID / 정류장 ID / 날짜 필터링")
    fun testGetBusArrivalLogsWithFilters() {
        whenever(
            logRepository.findByRouteIDAndStopIDAndDepartureDateIsIn(
                216000068,
                216000138,
                listOf(LocalDate.parse("2021-03-15")),
            ),
        ).thenReturn(
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
        )
        val result = routeService.getBusDepartureLogByRouteStopAndDate(216000068, 216000138, listOf(LocalDate.parse("2021-03-15")))
        assertEquals(
            true,
            result.all {
                it.routeID == 216000068 &&
                    it.stopID == 216000138 &&
                    it.departureDate == LocalDate.parse("2021-03-15")
            },
        )
    }

    @Test
    @DisplayName("버스 도착 로그 조회 - 존재하지 않는 노선 ID")
    fun testGetBusArrivalLogsNonExistentRouteID() {
        whenever(routeRepository.findById(216000999)).thenReturn(Optional.empty())
        assertThrows<BusRouteNotFoundException> {
            routeService.getBusDepartureLogByRouteStop(216000999, 2)
        }
    }

    @Test
    @DisplayName("버스 도착 로그 조회 - 존재하지 않는 노선 정류장 Seq")
    fun testGetBusArrivalLogsNonExistentRouteStopSeq() {
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        whenever(routeStopRepository.findByRouteIDAndSeq(216000068, 99)).thenReturn(null)
        assertThrows<BusRouteStopNotFoundException> {
            routeService.getBusDepartureLogByRouteStop(216000068, 99)
        }
    }

    @Test
    @DisplayName("버스 출발 로그 배치 조회 - 빈 키")
    fun testGetBusDepartureLogBatchEmptyKeys() {
        val result = routeService.getBusDepartureLogBatch(emptySet())
        assertEquals(0, result.size)
    }

    @Test
    @DisplayName("버스 출발 로그 배치 조회 - 빈 날짜 목록")
    fun testGetBusDepartureLogBatchEmptyDates() {
        val keys =
            setOf(
                BusDepartureLogKey(routeID = 216000068, stopID = 216000138, dates = emptyList()),
                BusDepartureLogKey(routeID = 216000068, stopID = 216000358, dates = emptyList()),
            )

        val result = routeService.getBusDepartureLogBatch(keys)

        assertEquals(2, result.size)
        assertEquals(0, result[BusDepartureLogKey(routeID = 216000068, stopID = 216000138, dates = emptyList())]?.size)
        assertEquals(0, result[BusDepartureLogKey(routeID = 216000068, stopID = 216000358, dates = emptyList())]?.size)
    }

    @Test
    @DisplayName("버스 출발 로그 배치 조회 - 정상")
    fun testGetBusDepartureLogBatch() {
        val dates = listOf(LocalDate.parse("2025-03-01"), LocalDate.parse("2025-03-02"))
        val keys =
            setOf(
                BusDepartureLogKey(routeID = 216000068, stopID = 216000138, dates = dates),
                BusDepartureLogKey(routeID = 216000068, stopID = 216000358, dates = listOf(LocalDate.parse("2025-03-01"))),
            )
        whenever(
            logRepository.findByRouteIDInAndStopIDInAndDepartureDateIn(
                listOf(216000068),
                listOf(216000138, 216000358),
                listOf(LocalDate.parse("2025-03-01"), LocalDate.parse("2025-03-02")),
            ),
        ).thenReturn(
            listOf(
                BusDepartureLog(
                    seq = 1,
                    routeID = 216000068,
                    stopID = 216000138,
                    departureDate = LocalDate.parse("2025-03-01"),
                    departureTime = LocalTime.parse("05:30:00"),
                    vehicleID = "123가4567",
                    routeStop = null,
                ),
                BusDepartureLog(
                    seq = 2,
                    routeID = 216000068,
                    stopID = 216000138,
                    departureDate = LocalDate.parse("2025-03-02"),
                    departureTime = LocalTime.parse("06:00:00"),
                    vehicleID = "234나5678",
                    routeStop = null,
                ),
                BusDepartureLog(
                    seq = 3,
                    routeID = 216000068,
                    stopID = 216000358,
                    departureDate = LocalDate.parse("2025-03-01"),
                    departureTime = LocalTime.parse("07:00:00"),
                    vehicleID = "345다6789",
                    routeStop = null,
                ),
            ),
        )

        val result = routeService.getBusDepartureLogBatch(keys)

        assertEquals(2, result.size)
        assertEquals(2, result[BusDepartureLogKey(routeID = 216000068, stopID = 216000138, dates = dates)]?.size)
        assertEquals(
            1,
            result[BusDepartureLogKey(routeID = 216000068, stopID = 216000358, dates = listOf(LocalDate.parse("2025-03-01")))]?.size,
        )
    }

    @Test
    @DisplayName("버스 출발 로그 배치 조회 - 결과 없음")
    fun testGetBusDepartureLogBatchNoResult() {
        val keys =
            setOf(
                BusDepartureLogKey(routeID = 216000068, stopID = 216000999, dates = listOf(LocalDate.parse("2025-03-01"))),
            )
        whenever(
            logRepository.findByRouteIDInAndStopIDInAndDepartureDateIn(
                listOf(216000068),
                listOf(216000999),
                listOf(LocalDate.parse("2025-03-01")),
            ),
        ).thenReturn(emptyList())

        val result = routeService.getBusDepartureLogBatch(keys)

        assertEquals(1, result.size)
        assertEquals(
            0,
            result[BusDepartureLogKey(routeID = 216000068, stopID = 216000999, dates = listOf(LocalDate.parse("2025-03-01")))]?.size,
        )
    }

    @Test
    @DisplayName("버스 출발 로그 배치 조회 - 여러 날짜 중 일부만 매칭")
    fun testGetBusDepartureLogBatchPartialDateMatch() {
        val dates = listOf(LocalDate.parse("2025-03-01"), LocalDate.parse("2025-03-02"))
        val keys =
            setOf(
                BusDepartureLogKey(routeID = 216000068, stopID = 216000138, dates = dates),
            )
        whenever(
            logRepository.findByRouteIDInAndStopIDInAndDepartureDateIn(
                listOf(216000068),
                listOf(216000138),
                dates,
            ),
        ).thenReturn(
            listOf(
                BusDepartureLog(
                    seq = 1,
                    routeID = 216000068,
                    stopID = 216000138,
                    departureDate = LocalDate.parse("2025-03-01"),
                    departureTime = LocalTime.parse("05:30:00"),
                    vehicleID = "123가4567",
                    routeStop = null,
                ),
            ),
        )

        val result = routeService.getBusDepartureLogBatch(keys)

        assertEquals(1, result.size)
        assertEquals(1, result[BusDepartureLogKey(routeID = 216000068, stopID = 216000138, dates = dates)]?.size)
        assertEquals("123가4567", result[BusDepartureLogKey(routeID = 216000068, stopID = 216000138, dates = dates)]?.get(0)?.vehicleID)
    }

    @Test
    @DisplayName("버스 정류장 목록 조회")
    fun testGetAllBusStops() {
        whenever(stopRepository.findAll()).thenReturn(listOf(TEST_STOP_1, TEST_STOP_2))
        val result = stopService.getBusStopList()
        assertEquals(2, result.size)
        assertEquals(216000358, result[0].id)
        assertEquals(216000138, result[1].id)
    }

    @Test
    @DisplayName("버스 정류장 항목 생성")
    fun testCreateBusStop() {
        val payload =
            CreateBusStopRequest(
                id = 216000139,
                name = "테스트정류장",
                districtCode = 2,
                latitude = 37.785000,
                longitude = 127.051000,
                mobileNumber = "00000",
                regionName = "경기",
            )
        whenever(stopRepository.findById(216000139)).thenReturn(Optional.empty())
        whenever(
            stopRepository.save(
                BusStop(
                    id = 216000139,
                    name = "테스트정류장",
                    districtCode = 2,
                    latitude = 37.785000,
                    longitude = 127.051000,
                    mobileNumber = "00000",
                    regionName = "경기",
                    busRoutes = emptyList(),
                    startBusRoutes = emptyList(),
                ),
            ),
        ).thenReturn(
            BusStop(
                id = 216000139,
                name = "테스트정류장",
                districtCode = 2,
                latitude = 37.785000,
                longitude = 127.051000,
                mobileNumber = "00000",
                regionName = "경기",
                busRoutes = emptyList(),
                startBusRoutes = emptyList(),
            ),
        )
        val result = stopService.createBusStop(payload)
        assertEquals(216000139, result.id)
        assertEquals("테스트정류장", result.name)
    }

    @Test
    @DisplayName("버스 정류장 항목 생성 - 중복된 ID")
    fun testCreateBusStopDuplicateID() {
        val payload =
            CreateBusStopRequest(
                id = 216000138,
                name = "테스트정류장",
                districtCode = 2,
                latitude = 37.785000,
                longitude = 127.051000,
                mobileNumber = "00000",
                regionName = "경기",
            )
        whenever(stopRepository.findById(216000138)).thenReturn(Optional.of(TEST_STOP_2))
        assertThrows<DuplicateBusStopException> {
            stopService.createBusStop(payload)
        }
    }

    @Test
    @DisplayName("버스 정류장 항목 조회")
    fun testGetBusStopById() {
        whenever(stopRepository.findById(216000138)).thenReturn(Optional.of(TEST_STOP_2))
        val stop = stopService.getBusStopById(216000138)
        assertEquals(216000138, stop.id)
    }

    @Test
    @DisplayName("버스 정류장 항목 조회 - 존재하지 않는 ID")
    fun testGetBusStopByIdNonExistentID() {
        whenever(stopRepository.findById(216000999)).thenReturn(Optional.empty())
        assertThrows<BusStopNotFoundException> {
            stopService.getBusStopById(216000999)
        }
    }

    @Test
    @DisplayName("버스 정류장 항목 수정")
    fun testUpdateBusStop() {
        val payload =
            UpdateBusStopRequest(
                name = "수정된정류장",
                districtCode = 2,
                latitude = 37.785000,
                longitude = 127.051000,
                mobileNumber = "00000",
                regionName = "경기",
            )
        val updatedStop =
            TEST_STOP_2.copy(
                name = "수정된정류장",
                districtCode = 2,
                latitude = 37.785000,
                longitude = 127.051000,
                mobileNumber = "00000",
                regionName = "경기",
            )
        whenever(stopRepository.findById(216000138)).thenReturn(Optional.of(TEST_STOP_2))
        whenever(
            stopRepository.save(
                TEST_STOP_2.copy(
                    name = "수정된정류장",
                    districtCode = 2,
                    latitude = 37.785000,
                    longitude = 127.051000,
                    mobileNumber = "00000",
                    regionName = "경기",
                ),
            ),
        ).thenReturn(updatedStop)
        val result = stopService.updateBusStop(216000138, payload)
        assertEquals(216000138, result.id)
        assertEquals("수정된정류장", result.name)
    }

    @Test
    @DisplayName("버스 정류장 항목 수정 - 존재하지 않는 ID")
    fun testUpdateBusStopNonExistentID() {
        val payload =
            UpdateBusStopRequest(
                name = "수정된정류장",
                districtCode = 2,
                latitude = 37.785000,
                longitude = 127.051000,
                mobileNumber = "00000",
                regionName = "경기",
            )
        whenever(stopRepository.findById(216000999)).thenReturn(Optional.empty())
        assertThrows<BusStopNotFoundException> {
            stopService.updateBusStop(216000999, payload)
        }
    }

    @Test
    @DisplayName("버스 정류장 항목 삭제")
    fun testDeleteBusStop() {
        whenever(stopRepository.findById(216000138)).thenReturn(Optional.of(TEST_STOP_2))
        stopService.deleteBusStopById(216000138)
    }

    @Test
    @DisplayName("버스 정류장 항목 삭제 - 존재하지 않는 ID")
    fun testDeleteBusStopNonExistentID() {
        whenever(stopRepository.findById(216000999)).thenReturn(Optional.empty())
        assertThrows<BusStopNotFoundException> {
            stopService.deleteBusStopById(216000999)
        }
    }

    @Test
    @DisplayName("버스 시간표 목록 조회")
    fun testGetAllBusTimetables() {
        whenever(
            timetableRepository.findAll(
                Sort.by(
                    Sort.Order.asc("routeID"),
                    Sort.Order.asc("startStopID"),
                    Sort.Order.asc("departureTime"),
                ),
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
        val result =
            timetableService.getBusTimetableList(
                null,
                null,
            )
        assertEquals(2, result.size)
        assertEquals(216000068, result[0].routeID)
        assertEquals(LocalTime.parse("05:30:00"), result[0].departureTime)
        assertEquals(216000068, result[1].routeID)
        assertEquals(LocalTime.parse("06:00:00"), result[1].departureTime)
        val result2 =
            timetableService.getBusTimetableList(
                null,
                null,
                emptyList(),
            )
        assertEquals(2, result2.size)
        assertEquals(216000068, result2[0].routeID)
        assertEquals(LocalTime.parse("05:30:00"), result2[0].departureTime)
        assertEquals(216000068, result2[1].routeID)
        assertEquals(LocalTime.parse("06:00:00"), result2[1].departureTime)
    }

    @Test
    @DisplayName("버스 시간표 목록 조회 - 노선 ID 필터링")
    fun testGetBusTimetablesByRouteID() {
        whenever(
            timetableRepository.findByRouteID(
                216000068,
                Sort.by(
                    Sort.Order.asc("routeID"),
                    Sort.Order.asc("startStopID"),
                    Sort.Order.asc("departureTime"),
                ),
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
        val result =
            timetableService.getBusTimetableList(
                216000068,
                null,
            )
        assertEquals(2, result.size)
        assertEquals(216000068, result[0].routeID)
        assertEquals(LocalTime.parse("05:30:00"), result[0].departureTime)
        assertEquals(216000068, result[1].routeID)
        assertEquals(LocalTime.parse("06:00:00"), result[1].departureTime)
        val result2 =
            timetableService.getBusTimetableList(
                216000068,
                null,
                emptyList(),
            )
        assertEquals(2, result2.size)
        assertEquals(216000068, result2[0].routeID)
        assertEquals(LocalTime.parse("05:30:00"), result2[0].departureTime)
        assertEquals(216000068, result2[1].routeID)
        assertEquals(LocalTime.parse("06:00:00"), result2[1].departureTime)
    }

    @Test
    @DisplayName("버스 시간표 목록 조회 - 평일/주말 필터링")
    fun testGetBusTimetablesByWeekdays() {
        whenever(
            timetableRepository.findByWeekdayIsIn(
                listOf("weekdays"),
                Sort.by(
                    Sort.Order.asc("routeID"),
                    Sort.Order.asc("startStopID"),
                    Sort.Order.asc("departureTime"),
                ),
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
        val result = timetableService.getBusTimetableList(null, null, listOf("weekdays"))
        assertEquals(true, result.all { it.weekday == "weekdays" })
    }

    @Test
    @DisplayName("버스 시간표 목록 조회 - 정류장 ID 필터링")
    fun testGetBusTimetablesByStopID() {
        whenever(
            timetableRepository.findByStartStopID(
                216000358,
                Sort.by(
                    Sort.Order.asc("routeID"),
                    Sort.Order.asc("startStopID"),
                    Sort.Order.asc("departureTime"),
                ),
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
        val result =
            timetableService.getBusTimetableList(
                null,
                216000358,
            )
        assertEquals(2, result.size)
        assertEquals(216000068, result[0].routeID)
        assertEquals(LocalTime.parse("05:30:00"), result[0].departureTime)
        assertEquals(216000068, result[1].routeID)
        assertEquals(LocalTime.parse("06:00:00"), result[1].departureTime)
        val result2 =
            timetableService.getBusTimetableList(
                null,
                216000358,
                emptyList(),
            )
        assertEquals(2, result2.size)
        assertEquals(216000068, result2[0].routeID)
        assertEquals(LocalTime.parse("05:30:00"), result2[0].departureTime)
        assertEquals(216000068, result2[1].routeID)
        assertEquals(LocalTime.parse("06:00:00"), result2[1].departureTime)
    }

    @Test
    @DisplayName("버스 시간표 목록 조회 - 노선 ID 및 정류장 ID 필터링")
    fun testGetBusTimetablesByRouteIDAndStopID() {
        whenever(
            timetableRepository.findByRouteIDAndStartStopID(
                216000068,
                216000358,
                Sort.by(
                    Sort.Order.asc("routeID"),
                    Sort.Order.asc("startStopID"),
                    Sort.Order.asc("departureTime"),
                ),
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
        val result =
            timetableService.getBusTimetableList(
                216000068,
                216000358,
            )
        assertEquals(2, result.size)
        assertEquals(216000068, result[0].routeID)
        assertEquals(LocalTime.parse("05:30:00"), result[0].departureTime)
        assertEquals(216000068, result[1].routeID)
        assertEquals(LocalTime.parse("06:00:00"), result[1].departureTime)
        val result2 =
            timetableService.getBusTimetableList(
                216000068,
                216000358,
                emptyList(),
            )
        assertEquals(2, result2.size)
        assertEquals(216000068, result2[0].routeID)
        assertEquals(LocalTime.parse("05:30:00"), result2[0].departureTime)
        assertEquals(216000068, result2[1].routeID)
        assertEquals(LocalTime.parse("06:00:00"), result2[1].departureTime)
    }

    @Test
    @DisplayName("버스 시간표 목록 조회 - 노선 ID 및 평일/주말 필터링")
    fun testGetBusTimetablesByRouteIDAndWeekdays() {
        whenever(
            timetableRepository.findByRouteIDAndWeekdayIsIn(
                216000068,
                listOf("weekdays"),
                Sort.by(
                    Sort.Order.asc("routeID"),
                    Sort.Order.asc("startStopID"),
                    Sort.Order.asc("departureTime"),
                ),
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
        val result = timetableService.getBusTimetableList(216000068, null, listOf("weekdays"))
        assertEquals(true, result.all { it.weekday == "weekdays" && it.routeID == 216000068 })
    }

    @Test
    @DisplayName("버스 시간표 목록 조회 - 노선 ID 및 정류장 ID 필터링")
    fun testGetBusTimetablesByWeekdaysAndStopID() {
        whenever(
            timetableRepository.findByStartStopIDAndWeekdayIsIn(
                216000358,
                listOf("weekdays"),
                Sort.by(
                    Sort.Order.asc("routeID"),
                    Sort.Order.asc("startStopID"),
                    Sort.Order.asc("departureTime"),
                ),
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
        val result = timetableService.getBusTimetableList(null, 216000358, listOf("weekdays"))
        assertEquals(true, result.all { it.weekday == "weekdays" && it.startStopID == 216000358 })
    }

    @Test
    @DisplayName("버스 시간표 항목 목록 조회 - 노선 ID, 정류장 ID, 평일/주말 필터링")
    fun testGetBusTimetablesByRouteIDAndWeekdayAndStopID() {
        whenever(
            timetableRepository.findByRouteIDAndStartStopIDAndWeekdayIsIn(
                216000068,
                216000358,
                listOf("weekdays"),
                Sort.by(
                    Sort.Order.asc("routeID"),
                    Sort.Order.asc("startStopID"),
                    Sort.Order.asc("departureTime"),
                ),
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
        val result = timetableService.getBusTimetableList(216000068, 216000358, listOf("weekdays"))
        assertEquals(true, result.all { it.weekday == "weekdays" && it.startStopID == 216000358 && it.routeID == 216000068 })
    }

    @Test
    @DisplayName("버스 시간표 배치 조회 - 빈 키")
    fun testGetBusTimetableBatchEmptyKeys() {
        val result = timetableService.getBusTimetableBatch(emptySet())
        assertEquals(0, result.size)
    }

    @Test
    @DisplayName("버스 시간표 배치 조회 - 정상")
    fun testGetBusTimetableBatch() {
        val keys =
            setOf(
                BusTimetableKey(routeID = 216000068, startStopID = 216000358, weekdays = null, after = null),
                BusTimetableKey(routeID = 216000068, startStopID = 216000138, weekdays = null, after = null),
            )
        val sort =
            Sort.by(
                Sort.Order.asc("routeID"),
                Sort.Order.asc("startStopID"),
                Sort.Order.asc("departureTime"),
            )
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndDepartureTimeAfter(
                listOf(216000068),
                listOf(216000358, 216000138),
                LocalTime.MIN,
                sort,
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
                    weekday = "saturday",
                    departureTime = LocalTime.parse("06:00:00"),
                ),
                BusTimetable(
                    seq = 3,
                    routeID = 216000068,
                    startStopID = 216000138,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("07:00:00"),
                ),
            ),
        )
        val result = timetableService.getBusTimetableBatch(keys)
        assertEquals(2, result.size)
        assertEquals(2, result[BusTimetableKey(routeID = 216000068, startStopID = 216000358, weekdays = null, after = null)]?.size)
        assertEquals(1, result[BusTimetableKey(routeID = 216000068, startStopID = 216000138, weekdays = null, after = null)]?.size)
    }

    @Test
    @DisplayName("버스 시간표 배치 조회 - 평일/주말 필터링")
    fun testGetBusTimetableBatchWithWeekdays() {
        val keys =
            setOf(
                BusTimetableKey(routeID = 216000068, startStopID = 216000358, weekdays = listOf("weekdays"), after = null),
            )
        val sort =
            Sort.by(
                Sort.Order.asc("routeID"),
                Sort.Order.asc("startStopID"),
                Sort.Order.asc("departureTime"),
            )
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndDepartureTimeAfter(
                listOf(216000068),
                listOf(216000358),
                LocalTime.MIN,
                sort,
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
                    weekday = "saturday",
                    departureTime = LocalTime.parse("06:00:00"),
                ),
                BusTimetable(
                    seq = 3,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "sunday",
                    departureTime = LocalTime.parse("07:00:00"),
                ),
            ),
        )
        val result = timetableService.getBusTimetableBatch(keys)
        val timetables = result[BusTimetableKey(routeID = 216000068, startStopID = 216000358, weekdays = listOf("weekdays"), after = null)]
        assertEquals(1, timetables?.size)
        assertEquals("weekdays", timetables?.get(0)?.weekday)
    }

    @Test
    @DisplayName("버스 시간표 배치 조회 - 시간 필터링")
    fun testGetBusTimetableBatchWithDepartureTime() {
        val keys =
            setOf(
                BusTimetableKey(routeID = 216000068, startStopID = 216000358, weekdays = null, after = LocalTime.parse("06:00:00")),
            )
        val sort =
            Sort.by(
                Sort.Order.asc("routeID"),
                Sort.Order.asc("startStopID"),
                Sort.Order.asc("departureTime"),
            )
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndDepartureTimeAfter(
                listOf(216000068),
                listOf(216000358),
                LocalTime.MIN,
                sort,
            ),
        ).thenReturn(
            listOf(
                BusTimetable(
                    seq = 2,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "sunday",
                    departureTime = LocalTime.parse("05:00:00"),
                ),
                BusTimetable(
                    seq = 3,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "sunday",
                    departureTime = LocalTime.parse("07:00:00"),
                ),
            ),
        )
        val result = timetableService.getBusTimetableBatch(keys)
        val timetables =
            result[
                BusTimetableKey(
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekdays = null,
                    after = LocalTime.parse("06:00:00"),
                ),
            ]
        assertEquals(1, timetables?.size)
        assertEquals("sunday", timetables?.get(0)?.weekday)
        assertEquals(LocalTime.parse("07:00:00"), timetables?.get(0)?.departureTime)
    }

    @Test
    @DisplayName("버스 시간표 배치 조회 - 자정 이후 시간 필터링")
    fun testGetBusTimetableBatchAfterMidnightFilter() {
        val keys =
            setOf(
                BusTimetableKey(
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekdays = listOf("weekdays"),
                    after = LocalTime.parse("23:00:00"),
                ),
            )
        val sort =
            Sort.by(
                Sort.Order.asc("routeID"),
                Sort.Order.asc("startStopID"),
                Sort.Order.asc("departureTime"),
            )
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndDepartureTimeAfter(
                listOf(216000068),
                listOf(216000358),
                LocalTime.MIN,
                sort,
            ),
        ).thenReturn(
            listOf(
                BusTimetable(
                    seq = 1,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("22:00:00"),
                ),
                BusTimetable(
                    seq = 2,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("23:30:00"),
                ),
                BusTimetable(
                    seq = 3,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("00:30:00"),
                ),
                BusTimetable(
                    seq = 4,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("01:00:00"),
                ),
            ),
        )
        val result = timetableService.getBusTimetableBatch(keys)
        val key =
            BusTimetableKey(
                routeID = 216000068,
                startStopID = 216000358,
                weekdays = listOf("weekdays"),
                after = LocalTime.parse("23:00:00"),
            )
        val timetables = result[key]
        // 22:00 excluded (before 23:00 in service-day order); 23:30, 00:30, 01:00 all included
        assertEquals(3, timetables?.size)
        assertEquals(LocalTime.parse("23:30:00"), timetables?.get(0)?.departureTime)
        assertEquals(LocalTime.parse("00:30:00"), timetables?.get(1)?.departureTime)
        assertEquals(LocalTime.parse("01:00:00"), timetables?.get(2)?.departureTime)
    }

    @Test
    @DisplayName("버스 시간표 배치 조회 - 서비스 일 기준 정렬 (자정 이후 버스가 늦은 밤 버스 뒤에 위치)")
    fun testGetBusTimetableBatchServiceDayOrdering() {
        val keys =
            setOf(
                BusTimetableKey(routeID = 216000068, startStopID = 216000358, weekdays = listOf("weekdays"), after = null),
            )
        val sort =
            Sort.by(
                Sort.Order.asc("routeID"),
                Sort.Order.asc("startStopID"),
                Sort.Order.asc("departureTime"),
            )
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndDepartureTimeAfter(
                listOf(216000068),
                listOf(216000358),
                LocalTime.MIN,
                sort,
            ),
        ).thenReturn(
            listOf(
                // DB returns in LocalTime order: 00:30, 01:00 come before 05:00 and 23:50
                BusTimetable(
                    seq = 3,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("00:30:00"),
                ),
                BusTimetable(
                    seq = 4,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("01:00:00"),
                ),
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
                    departureTime = LocalTime.parse("23:50:00"),
                ),
            ),
        )
        val result = timetableService.getBusTimetableBatch(keys)
        val key = BusTimetableKey(routeID = 216000068, startStopID = 216000358, weekdays = listOf("weekdays"), after = null)
        val timetables = result[key]
        assertEquals(4, timetables?.size)
        // Service-day order: 05:30, 23:50, 00:30, 01:00
        assertEquals(LocalTime.parse("05:30:00"), timetables?.get(0)?.departureTime)
        assertEquals(LocalTime.parse("23:50:00"), timetables?.get(1)?.departureTime)
        assertEquals(LocalTime.parse("00:30:00"), timetables?.get(2)?.departureTime)
        assertEquals(LocalTime.parse("01:00:00"), timetables?.get(3)?.departureTime)
    }

    @Test
    @DisplayName("버스 시간표 배치 조회 - 평일/주말 및 시간 필터링")
    fun testGetBusTimetableBatchWithWeekdaysAndDepartureTime() {
        val keys =
            setOf(
                BusTimetableKey(
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekdays = listOf("weekdays"),
                    after = LocalTime.parse("06:00:00"),
                ),
                BusTimetableKey(
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekdays = listOf("saturday"),
                    after = LocalTime.parse("05:30:00"),
                ),
            )
        val sort =
            Sort.by(
                Sort.Order.asc("routeID"),
                Sort.Order.asc("startStopID"),
                Sort.Order.asc("departureTime"),
            )
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndDepartureTimeAfter(
                listOf(216000068),
                listOf(216000358),
                LocalTime.MIN,
                sort,
            ),
        ).thenReturn(
            listOf(
                BusTimetable(
                    seq = 1,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("06:30:00"),
                ),
                BusTimetable(
                    seq = 2,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "saturday",
                    departureTime = LocalTime.parse("05:45:00"),
                ),
                BusTimetable(
                    seq = 3,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("05:45:00"),
                ),
            ),
        )
        val result = timetableService.getBusTimetableBatch(keys)
        val timetables =
            result[
                BusTimetableKey(
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekdays = listOf("weekdays"),
                    after = LocalTime.parse("06:00:00"),
                ),
            ]
        assertEquals(1, timetables?.size)
        assertEquals("weekdays", timetables?.get(0)?.weekday)
        val timetables2 =
            result[
                BusTimetableKey(
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekdays = listOf("saturday"),
                    after = LocalTime.parse("05:30:00"),
                ),
            ]
        assertEquals(1, timetables2?.size)
        assertEquals("saturday", timetables2?.get(0)?.weekday)
    }

    @Test
    @DisplayName("버스 시간표 배치 조회 - 필터링 후 결과 없음")
    fun testGetBusTimetableBatchNoResultAfterFiltering() {
        val keys =
            setOf(
                BusTimetableKey(
                    routeID = 216000068,
                    startStopID = 216000999,
                    weekdays = listOf("sunday"),
                    after = null,
                ),
                BusTimetableKey(
                    routeID = 216000068,
                    startStopID = 216000999,
                    weekdays = listOf("weekdays"),
                    after = LocalTime.parse("23:00:00"),
                ),
            )
        val sort =
            Sort.by(
                Sort.Order.asc("routeID"),
                Sort.Order.asc("startStopID"),
                Sort.Order.asc("departureTime"),
            )
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndDepartureTimeAfter(
                listOf(216000068),
                listOf(216000999),
                LocalTime.MIN,
                sort,
            ),
        ).thenReturn(
            listOf(
                BusTimetable(
                    seq = 1,
                    routeID = 216000068,
                    startStopID = 216000999,
                    weekday = "sunday",
                    departureTime = LocalTime.parse("07:00:00"),
                ),
            ),
        )
        val result = timetableService.getBusTimetableBatch(keys)
        assertEquals(2, result.size)
        assertEquals(
            0,
            result[
                BusTimetableKey(
                    routeID = 216000068,
                    startStopID = 216000999,
                    weekdays = listOf("weekdays"),
                    after = LocalTime.parse("23:00:00"),
                ),
            ]?.size,
        )
    }

    @Test
    @DisplayName("버스 시간표 배치 조회 - 결과 없음")
    fun testGetBusTimetableBatchNoResult() {
        val keys =
            setOf(
                BusTimetableKey(routeID = 216000068, startStopID = 216000999, weekdays = null, after = null),
            )
        val sort =
            Sort.by(
                Sort.Order.asc("routeID"),
                Sort.Order.asc("startStopID"),
                Sort.Order.asc("departureTime"),
            )
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndDepartureTimeAfter(
                listOf(216000068),
                listOf(216000999),
                LocalTime.MIN,
                sort,
            ),
        ).thenReturn(emptyList())
        val result = timetableService.getBusTimetableBatch(keys)
        assertEquals(1, result.size)
        assertEquals(0, result[BusTimetableKey(routeID = 216000068, startStopID = 216000999, weekdays = null, after = null)]?.size)
    }

    @Test
    @DisplayName("버스 시간표 배치 조회 - 빈 weekdays 리스트")
    fun testGetBusTimetableBatchEmptyWeekdays() {
        val keys =
            setOf(
                BusTimetableKey(routeID = 216000068, startStopID = 216000358, weekdays = emptyList(), after = null),
            )
        val sort =
            Sort.by(
                Sort.Order.asc("routeID"),
                Sort.Order.asc("startStopID"),
                Sort.Order.asc("departureTime"),
            )
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndDepartureTimeAfter(
                listOf(216000068),
                listOf(216000358),
                LocalTime.MIN,
                sort,
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
                    weekday = "saturday",
                    departureTime = LocalTime.parse("06:00:00"),
                ),
            ),
        )
        val result = timetableService.getBusTimetableBatch(keys)
        assertEquals(2, result[BusTimetableKey(routeID = 216000068, startStopID = 216000358, weekdays = emptyList(), after = null)]?.size)
    }

    @Test
    @DisplayName("버스 시간표 항목 생성")
    fun testCreateBusTimetable() {
        val payload =
            BusTimetableRequest(
                routeID = 216000068,
                startStopID = 216000358,
                dayType = "weekdays",
                departureTime = "07:00:00",
            )
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        whenever(stopRepository.findById(216000358)).thenReturn(Optional.of(TEST_STOP_1))
        whenever(
            timetableRepository.save(
                BusTimetable(
                    seq = null,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("07:00:00"),
                ),
            ),
        ).thenReturn(
            BusTimetable(
                seq = 3,
                routeID = 216000068,
                startStopID = 216000358,
                weekday = "weekdays",
                departureTime = LocalTime.parse("07:00:00"),
            ),
        )
        val result = timetableService.createBusTimetable(payload)
        assertEquals(3, result.seq)
        assertEquals(216000068, result.routeID)
        assertEquals(LocalTime.parse("07:00:00"), result.departureTime)
    }

    @Test
    @DisplayName("버스 시간표 항목 생성 - 잘못된 시간 형식")
    fun testCreateBusTimetableInvalidTimeFormat() {
        val payload =
            BusTimetableRequest(
                routeID = 216000068,
                startStopID = 216000358,
                dayType = "weekdays",
                departureTime = "07:00",
            )
        assertThrows<LocalTimeNotValidException> {
            timetableService.createBusTimetable(payload)
        }
    }

    @Test
    @DisplayName("버스 시간표 항목 생성 - 존재하지 않는 노선 ID")
    fun testCreateBusTimetableNonExistentRouteID() {
        val payload =
            BusTimetableRequest(
                routeID = 216000999,
                startStopID = 216000358,
                dayType = "weekdays",
                departureTime = "07:00:00",
            )
        whenever(routeRepository.findById(216000999)).thenReturn(Optional.empty())
        assertThrows<BusRouteNotFoundException> {
            timetableService.createBusTimetable(payload)
        }
    }

    @Test
    @DisplayName("버스 시간표 항목 생성 - 존재하지 않는 기점 정류장 ID")
    fun testCreateBusTimetableNonExistentStartStopID() {
        val payload =
            BusTimetableRequest(
                routeID = 216000068,
                startStopID = 216000999,
                dayType = "weekdays",
                departureTime = "07:00:00",
            )
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        whenever(stopRepository.findById(216000999)).thenReturn(Optional.empty())
        assertThrows<BusStartStopNotFoundException> {
            timetableService.createBusTimetable(payload)
        }
    }

    @Test
    @DisplayName("버스 시간표 항목 생성 - 중복된 시간표")
    fun testCreateBusTimetableDuplicate() {
        val payload =
            BusTimetableRequest(
                routeID = 216000068,
                startStopID = 216000358,
                dayType = "weekdays",
                departureTime = "05:30:00",
            )
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        whenever(stopRepository.findById(216000358)).thenReturn(Optional.of(TEST_STOP_1))
        whenever(
            timetableRepository.findByRouteIDAndStartStopIDAndWeekdayAndDepartureTime(
                216000068,
                216000358,
                "weekdays",
                LocalTime.parse("05:30:00"),
            ),
        ).thenReturn(
            BusTimetable(
                seq = 1,
                routeID = 216000068,
                startStopID = 216000358,
                weekday = "weekdays",
                departureTime = LocalTime.parse("05:30:00"),
            ),
        )
        assertThrows<DuplicateBusTimetableException> {
            timetableService.createBusTimetable(payload)
        }
    }

    @Test
    @DisplayName("버스 시간표 항목 조회")
    fun testGetBusTimetableBySeq() {
        whenever(timetableRepository.findById(1)).thenReturn(
            Optional.of(
                BusTimetable(
                    seq = 1,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("05:30:00"),
                ),
            ),
        )
        val result = timetableService.getBusTimetableById(1)
        assertEquals(1, result.seq)
        assertEquals(216000068, result.routeID)
        assertEquals(LocalTime.parse("05:30:00"), result.departureTime)
    }

    @Test
    @DisplayName("버스 시간표 항목 조회 - 존재하지 않는 Seq")
    fun testGetBusTimetableBySeqNonExistent() {
        whenever(timetableRepository.findById(99)).thenReturn(Optional.empty())
        assertThrows<BusTimetableNotFoundException> {
            timetableService.getBusTimetableById(99)
        }
    }

    @Test
    @DisplayName("버스 시간표 항목 수정")
    fun testUpdateBusTimetable() {
        val payload =
            BusTimetableRequest(
                routeID = 216000068,
                startStopID = 216000358,
                dayType = "weekdays",
                departureTime = "08:00:00",
            )
        val updatedTimetable =
            BusTimetable(
                seq = 1,
                routeID = 216000068,
                startStopID = 216000358,
                weekday = "weekdays",
                departureTime = LocalTime.parse("08:00:00"),
            )
        whenever(timetableRepository.findById(1)).thenReturn(
            Optional.of(
                BusTimetable(
                    seq = 1,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("05:30:00"),
                ),
            ),
        )
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        whenever(stopRepository.findById(216000358)).thenReturn(Optional.of(TEST_STOP_1))
        whenever(
            timetableRepository.save(
                BusTimetable(
                    seq = 1,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("08:00:00"),
                ),
            ),
        ).thenReturn(updatedTimetable)
        val result = timetableService.updateBusTimetable(1, payload)
        assertEquals(1, result.seq)
        assertEquals(216000068, result.routeID)
        assertEquals(LocalTime.parse("08:00:00"), result.departureTime)
    }

    @Test
    @DisplayName("버스 시간표 항목 수정 - 존재하지 않는 시간표 Seq")
    fun testUpdateBusTimetableNonExistentSeq() {
        val payload =
            BusTimetableRequest(
                routeID = 216000068,
                startStopID = 216000358,
                dayType = "weekdays",
                departureTime = "08:00:00",
            )
        whenever(timetableRepository.findById(99)).thenReturn(Optional.empty())
        assertThrows<BusTimetableNotFoundException> {
            timetableService.updateBusTimetable(99, payload)
        }
    }

    @Test
    @DisplayName("버스 시간표 항목 수정 - 잘못된 시간 형식")
    fun testUpdateBusTimetableInvalidTimeFormat() {
        val payload =
            BusTimetableRequest(
                routeID = 216000068,
                startStopID = 216000358,
                dayType = "weekdays",
                departureTime = "08:00",
            )
        whenever(timetableRepository.findById(1)).thenReturn(
            Optional.of(
                BusTimetable(
                    seq = 1,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("05:30:00"),
                ),
            ),
        )
        assertThrows<LocalTimeNotValidException> {
            timetableService.updateBusTimetable(1, payload)
        }
    }

    @Test
    @DisplayName("버스 시간표 항목 수정 - 존재하지 않는 노선 ID")
    fun testUpdateBusTimetableNonExistentRouteID() {
        val payload =
            BusTimetableRequest(
                routeID = 216000999,
                startStopID = 216000358,
                dayType = "weekdays",
                departureTime = "08:00:00",
            )
        whenever(timetableRepository.findById(1)).thenReturn(
            Optional.of(
                BusTimetable(
                    seq = 1,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("05:30:00"),
                ),
            ),
        )
        whenever(routeRepository.findById(216000999)).thenReturn(Optional.empty())
        assertThrows<BusRouteNotFoundException> {
            timetableService.updateBusTimetable(1, payload)
        }
    }

    @Test
    @DisplayName("버스 시간표 항목 수정 - 존재하지 않는 기점 정류장 ID")
    fun testUpdateBusTimetableNonExistentStartStopID() {
        val payload =
            BusTimetableRequest(
                routeID = 216000068,
                startStopID = 216000999,
                dayType = "weekdays",
                departureTime = "08:00:00",
            )
        whenever(timetableRepository.findById(1)).thenReturn(
            Optional.of(
                BusTimetable(
                    seq = 1,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("05:30:00"),
                ),
            ),
        )
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        whenever(stopRepository.findById(216000999)).thenReturn(Optional.empty())
        assertThrows<BusStartStopNotFoundException> {
            timetableService.updateBusTimetable(1, payload)
        }
    }

    @Test
    @DisplayName("버스 시간표 항목 수정 - 중복된 시간표")
    fun testUpdateBusTimetableDuplicate() {
        val payload =
            BusTimetableRequest(
                routeID = 216000068,
                startStopID = 216000358,
                dayType = "weekdays",
                departureTime = "06:00:00",
            )
        whenever(timetableRepository.findById(1)).thenReturn(
            Optional.of(
                BusTimetable(
                    seq = 1,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("05:30:00"),
                ),
            ),
        )
        whenever(routeRepository.findById(216000068)).thenReturn(Optional.of(TEST_ROUTE_1))
        whenever(stopRepository.findById(216000358)).thenReturn(Optional.of(TEST_STOP_1))
        whenever(
            timetableRepository.findByRouteIDAndStartStopIDAndWeekdayAndDepartureTime(
                216000068,
                216000358,
                "weekdays",
                LocalTime.parse("06:00:00"),
            ),
        ).thenReturn(
            BusTimetable(
                seq = 2,
                routeID = 216000068,
                startStopID = 216000358,
                weekday = "weekdays",
                departureTime = LocalTime.parse("06:00:00"),
            ),
        )
        assertThrows<DuplicateBusTimetableException> {
            timetableService.updateBusTimetable(1, payload)
        }
    }

    @Test
    @DisplayName("버스 시간표 항목 삭제")
    fun testDeleteBusTimetable() {
        whenever(timetableRepository.findById(1)).thenReturn(
            Optional.of(
                BusTimetable(
                    seq = 1,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("05:30:00"),
                ),
            ),
        )
        timetableService.deleteBusTimetableById(1)
        verify(timetableRepository).delete(
            BusTimetable(
                seq = 1,
                routeID = 216000068,
                startStopID = 216000358,
                weekday = "weekdays",
                departureTime = LocalTime.parse("05:30:00"),
            ),
        )
    }

    @Test
    @DisplayName("버스 시간표 항목 삭제 - 존재하지 않는 ID")
    fun testDeleteBusTimetableNonExistentID() {
        whenever(timetableRepository.findById(99)).thenReturn(Optional.empty())
        assertThrows<BusTimetableNotFoundException> {
            timetableService.deleteBusTimetableById(99)
        }
    }

    @Test
    @DisplayName("버스 실시간 도착 정보 조회")
    fun testGetRealtimeList() {
        whenever(realtimeRepository.findAll()).thenReturn(
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
        val result = realtimeService.getBusRealtimeList()
        assertEquals(2, result.size)
        assertEquals(216000068, result[0].routeID)
        assertEquals(216000138, result[0].stopID)
        assertEquals(216000068, result[1].routeID)
        assertEquals(216000139, result[1].stopID)
    }

    @Test
    @DisplayName("버스 정류소별 실시간 도착 정보 조회")
    fun testGetRealtimeListByBusRouteStop() {
        whenever(
            realtimeRepository.findByRouteIDAndStopID(
                routeID = 216000068,
                stopID = 216000138,
            ),
        ).thenReturn(
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
                    stopID = 216000138,
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
        val result =
            realtimeService.getBusRealtimeListByBusStop(
                routeID = 216000068,
                stopID = 216000138,
            )
        assertEquals(2, result.size)
        assertEquals(216000068, result[0].routeID)
        assertEquals(216000138, result[0].stopID)
        assertEquals(216000068, result[1].routeID)
        assertEquals(216000138, result[1].stopID)
    }

    @Test
    @DisplayName("버스 실시간 도착 정보 배치 조회 - 빈 키")
    fun testGetBusRealtimeBatchEmptyKeys() {
        val result = realtimeService.getBusRealtimeBatch(emptySet())
        assertEquals(0, result.size)
    }

    @Test
    @DisplayName("버스 실시간 도착 정보 배치 조회 - 정상")
    fun testGetBusRealtimeBatch() {
        val keys =
            setOf(
                216000068 to 216000138,
                216000068 to 216000358,
            )
        whenever(
            realtimeRepository.findByRouteIDInAndStopIDIn(
                listOf(216000068),
                listOf(216000138, 216000358),
            ),
        ).thenReturn(
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
                    stopID = 216000138,
                    order = 2,
                    remainingTime = Duration.ofMinutes(15),
                    remainingStop = 5,
                    remainingSeat = 20,
                    isLowFloor = false,
                    updatedAt = ZonedDateTime.now(),
                    routeStop = null,
                ),
                BusRealtime(
                    routeID = 216000068,
                    stopID = 216000358,
                    order = 1,
                    remainingTime = Duration.ofMinutes(10),
                    remainingStop = 3,
                    remainingSeat = 30,
                    isLowFloor = false,
                    updatedAt = ZonedDateTime.now(),
                    routeStop = null,
                ),
            ),
        )

        val result = realtimeService.getBusRealtimeBatch(keys)

        assertEquals(2, result.size)
        assertEquals(2, result[216000068 to 216000138]?.size)
        assertEquals(1, result[216000068 to 216000358]?.size)
    }

    @Test
    @DisplayName("버스 실시간 도착 정보 배치 조회 - 결과 없음")
    fun testGetBusRealtimeBatchNoResult() {
        val keys = setOf(216000068 to 216000999)
        whenever(
            realtimeRepository.findByRouteIDInAndStopIDIn(
                listOf(216000068),
                listOf(216000999),
            ),
        ).thenReturn(emptyList())

        val result = realtimeService.getBusRealtimeBatch(keys)

        assertEquals(1, result.size)
        assertEquals(0, result[216000068 to 216000999]?.size)
    }

    @Test
    @DisplayName("버스 도착 정보 배치 조회 - 빈 키")
    fun testGetArrivalBatchEmptyKeys() {
        val result = realtimeService.getArrivalBatch(emptySet())
        assertEquals(0, result.size)
    }

    @Test
    @DisplayName("버스 도착 정보 배치 조회 - 실시간 + 시간표")
    fun testGetArrivalBatch() {
        val key =
            BusArrivalKey(
                routeID = 216000068,
                stopID = 216000138,
                startStopID = 216000358,
                limit = null,
            )
        whenever(
            realtimeRepository.findByRouteIDInAndStopIDIn(
                listOf(216000068),
                listOf(216000138),
            ),
        ).thenReturn(
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
                    stopID = 216000138,
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
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndWeekdayAndDepartureTimeAfter(
                any(),
                any(),
                any(),
                any(),
                any(),
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
                BusTimetable(
                    seq = 3,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("07:00:00"),
                ),
            ),
        )
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndWeekdayAndDepartureTimeBefore(
                any(),
                any(),
                any(),
                any(),
                any(),
            ),
        ).thenReturn(emptyList())

        val result = realtimeService.getArrivalBatch(setOf(key))

        assertEquals(1, result.size)
        val arrivals = result[key]!!
        assertEquals(5, arrivals.size)
        assertEquals(true, arrivals[0].isRealtime)
        assertEquals(true, arrivals[1].isRealtime)
        assertEquals(false, arrivals[2].isRealtime)
    }

    @Test
    @DisplayName("버스 도착 정보 배치 조회 - limit 적용")
    fun testGetArrivalBatchWithLimit() {
        val key =
            BusArrivalKey(
                routeID = 216000068,
                stopID = 216000138,
                startStopID = 216000358,
                limit = 2,
            )
        whenever(
            realtimeRepository.findByRouteIDInAndStopIDIn(
                listOf(216000068),
                listOf(216000138),
            ),
        ).thenReturn(
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
                    stopID = 216000138,
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
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndWeekdayAndDepartureTimeAfter(
                any(),
                any(),
                any(),
                any(),
                any(),
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
            ),
        )

        val result = realtimeService.getArrivalBatch(setOf(key))

        val arrivals = result[key]!!
        assertEquals(2, arrivals.size)
        assertEquals(true, arrivals[0].isRealtime)
        assertEquals(true, arrivals[1].isRealtime)
    }

    @Test
    @DisplayName("버스 도착 정보 배치 조회 - 결과 없음")
    fun testGetArrivalBatchNoResult() {
        val key =
            BusArrivalKey(
                routeID = 216000068,
                stopID = 216000999,
                startStopID = 216000358,
                limit = null,
            )
        whenever(
            realtimeRepository.findByRouteIDInAndStopIDIn(
                listOf(216000068),
                listOf(216000999),
            ),
        ).thenReturn(emptyList())
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndWeekdayAndDepartureTimeAfter(
                any(),
                any(),
                any(),
                any(),
                any(),
            ),
        ).thenReturn(emptyList())

        val result = realtimeService.getArrivalBatch(setOf(key))

        assertEquals(1, result.size)
        assertEquals(0, result[key]!!.size)
    }

    @Test
    @DisplayName("버스 도착 정보 배치 조회 - 다중 키")
    fun testGetArrivalBatchMultipleKeys() {
        val key1 =
            BusArrivalKey(
                routeID = 216000068,
                stopID = 216000138,
                startStopID = 216000358,
                limit = null,
            )
        val key2 =
            BusArrivalKey(
                routeID = 216000069,
                stopID = 216000358,
                startStopID = 216000138,
                limit = null,
            )
        whenever(
            realtimeRepository.findByRouteIDInAndStopIDIn(
                listOf(216000068, 216000069),
                listOf(216000138, 216000358),
            ),
        ).thenReturn(
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
                    routeID = 216000069,
                    stopID = 216000358,
                    order = 1,
                    remainingTime = Duration.ofMinutes(10),
                    remainingStop = 3,
                    remainingSeat = 30,
                    isLowFloor = false,
                    updatedAt = ZonedDateTime.now(),
                    routeStop = null,
                ),
            ),
        )
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndWeekdayAndDepartureTimeAfter(
                any(),
                any(),
                any(),
                any(),
                any(),
            ),
        ).thenReturn(emptyList())

        val result = realtimeService.getArrivalBatch(setOf(key1, key2))

        assertEquals(2, result.size)
        assertEquals(1, result[key1]!!.size)
        assertEquals(true, result[key1]!![0].isRealtime)
        assertEquals(1, result[key2]!!.size)
        assertEquals(true, result[key2]!![0].isRealtime)
    }

    // ── After-midnight branch (currentTime < SERVICE_DAY_START = 04:00) ─────────
    // Uses spy(realtimeService) + doReturn(fixedTime).whenever(spy).currentTime()
    // to control the clock without changing the production API.

    @Test
    @DisplayName("버스 도착 정보 배치 조회 - 자정 이후 (01:30): 이전 서비스 날 weekday 사용")
    fun testGetArrivalBatchAfterMidnightUsesServiceDayWeekday() {
        // Monday 2025-03-03 01:30 KST → serviceDate = Sunday 2025-03-02 → weekday = "sunday"
        val fixedNow = LocalDateTime.of(2025, 3, 3, 1, 30)
        val spyService = spy(realtimeService)
        doReturn(fixedNow).whenever(spyService).currentTime()

        val key = BusArrivalKey(routeID = 216000068, stopID = 216000138, startStopID = 216000358, limit = null)
        whenever(realtimeRepository.findByRouteIDInAndStopIDIn(any(), any())).thenReturn(emptyList())
        // The code calls findByRouteIDInAndStartStopIDInAndWeekdayAndDepartureTimeAfter with weekday="sunday"
        // and then filters departureTime < SERVICE_DAY_START (04:00)
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndWeekdayAndDepartureTimeAfter(
                listOf(216000068),
                listOf(216000358),
                "sunday",
                LocalTime.of(1, 30),
                Sort.by(Sort.Order.asc("departureTime")),
            ),
        ).thenReturn(
            listOf(
                // departureTime < 04:00 → kept after filter
                BusTimetable(
                    seq = 1,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "sunday",
                    departureTime = LocalTime.parse("02:00:00"),
                ),
                BusTimetable(
                    seq = 2,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "sunday",
                    departureTime = LocalTime.parse("02:30:00"),
                ),
                // departureTime >= 04:00 → removed by filter
                BusTimetable(
                    seq = 3,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "sunday",
                    departureTime = LocalTime.parse("05:00:00"),
                ),
            ),
        )

        val result = spyService.getArrivalBatch(setOf(key))
        val arrivals = result[key]!!

        assertEquals(2, arrivals.size)
        assertEquals(LocalTime.parse("02:00:00"), arrivals[0].time)
        assertEquals(LocalTime.parse("02:30:00"), arrivals[1].time)
    }

    @Test
    @DisplayName("버스 도착 정보 배치 조회 - 자정 이후 (01:30): 자정 이후 버스만 반환")
    fun testGetArrivalBatchAfterMidnightReturnsOnlyRemainingBuses() {
        // Sunday 2025-03-02 01:00 KST → serviceDate = Saturday 2025-03-01 → weekday = "saturday"
        val fixedNow = LocalDateTime.of(2025, 3, 2, 1, 0)
        val spyService = spy(realtimeService)
        doReturn(fixedNow).whenever(spyService).currentTime()

        val key = BusArrivalKey(routeID = 216000068, stopID = 216000138, startStopID = 216000358, limit = null)
        whenever(realtimeRepository.findByRouteIDInAndStopIDIn(any(), any())).thenReturn(emptyList())
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndWeekdayAndDepartureTimeAfter(
                listOf(216000068),
                listOf(216000358),
                "saturday",
                LocalTime.of(1, 0),
                Sort.by(Sort.Order.asc("departureTime")),
            ),
        ).thenReturn(
            listOf(
                BusTimetable(
                    seq = 1,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "saturday",
                    departureTime = LocalTime.parse("01:30:00"),
                ),
                BusTimetable(
                    seq = 2,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "saturday",
                    departureTime = LocalTime.parse("02:00:00"),
                ),
            ),
        )

        val result = spyService.getArrivalBatch(setOf(key))
        val arrivals = result[key]!!

        assertEquals(2, arrivals.size)
        assertEquals(LocalTime.parse("01:30:00"), arrivals[0].time)
        assertEquals(LocalTime.parse("02:00:00"), arrivals[1].time)
    }

    @Test
    @DisplayName("버스 도착 정보 배치 조회 - 자정 이후 (01:30): limit 적용")
    fun testGetArrivalBatchAfterMidnightWithLimit() {
        val fixedNow = LocalDateTime.of(2025, 3, 3, 1, 0)
        val spyService = spy(realtimeService)
        doReturn(fixedNow).whenever(spyService).currentTime()

        val key = BusArrivalKey(routeID = 216000068, stopID = 216000138, startStopID = 216000358, limit = 1)
        whenever(realtimeRepository.findByRouteIDInAndStopIDIn(any(), any())).thenReturn(emptyList())
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndWeekdayAndDepartureTimeAfter(
                any(),
                any(),
                any(),
                any(),
                any(),
            ),
        ).thenReturn(
            listOf(
                BusTimetable(
                    seq = 1,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "sunday",
                    departureTime = LocalTime.parse("01:30:00"),
                ),
                BusTimetable(
                    seq = 2,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "sunday",
                    departureTime = LocalTime.parse("02:00:00"),
                ),
            ),
        )

        val result = spyService.getArrivalBatch(setOf(key))
        val arrivals = result[key]!!

        assertEquals(1, arrivals.size)
        assertEquals(LocalTime.parse("01:30:00"), arrivals[0].time)
    }

    @Test
    @DisplayName("버스 도착 정보 배치 조회 - 자정 이후 (01:30): 결과 없음")
    fun testGetArrivalBatchAfterMidnightNoResult() {
        // 03:50 - just before service day start, no more buses
        val fixedNow = LocalDateTime.of(2025, 3, 3, 3, 50)
        val spyService = spy(realtimeService)
        doReturn(fixedNow).whenever(spyService).currentTime()

        val key = BusArrivalKey(routeID = 216000068, stopID = 216000138, startStopID = 216000358, limit = null)
        whenever(realtimeRepository.findByRouteIDInAndStopIDIn(any(), any())).thenReturn(emptyList())
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndWeekdayAndDepartureTimeAfter(
                any(),
                any(),
                any(),
                any(),
                any(),
            ),
        ).thenReturn(emptyList())

        val result = spyService.getArrivalBatch(setOf(key))
        assertEquals(0, result[key]!!.size)
    }

    @Test
    @DisplayName("weekday 결정 - 평일")
    fun testResolveWeekday() {
        val monday = LocalDate.of(2025, 3, 3)
        assertEquals("weekdays", realtimeService.resolveWeekday(monday))
    }

    @Test
    @DisplayName("weekday 결정 - 토요일")
    fun testResolveWeekdaySaturday() {
        val saturday = LocalDate.of(2025, 3, 1)
        assertEquals("saturday", realtimeService.resolveWeekday(saturday))
    }

    @Test
    @DisplayName("weekday 결정 - 일요일")
    fun testResolveWeekdaySunday() {
        val sunday = LocalDate.of(2025, 3, 2)
        assertEquals("sunday", realtimeService.resolveWeekday(sunday))
    }

    @Test
    @DisplayName("버스 도착 정보 배치 조회 - 자정 이후 버스 포함")
    fun testGetArrivalBatchIncludesAfterMidnightBuses() {
        val key =
            BusArrivalKey(
                routeID = 216000068,
                stopID = 216000138,
                startStopID = 216000358,
                limit = null,
            )
        whenever(
            realtimeRepository.findByRouteIDInAndStopIDIn(
                listOf(216000068),
                listOf(216000138),
            ),
        ).thenReturn(emptyList())
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndWeekdayAndDepartureTimeAfter(
                any(),
                any(),
                any(),
                any(),
                any(),
            ),
        ).thenReturn(
            listOf(
                BusTimetable(
                    seq = 1,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("23:30:00"),
                ),
                BusTimetable(
                    seq = 2,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("23:50:00"),
                ),
            ),
        )
        // After-midnight buses returned by DepartureTimeBefore(04:00) query
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndWeekdayAndDepartureTimeBefore(
                any(),
                any(),
                any(),
                any(),
                any(),
            ),
        ).thenReturn(
            listOf(
                BusTimetable(
                    seq = 3,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("00:30:00"),
                ),
                BusTimetable(
                    seq = 4,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("01:00:00"),
                ),
            ),
        )

        val result = realtimeService.getArrivalBatch(setOf(key))

        assertEquals(1, result.size)
        val arrivals = result[key]!!
        assertEquals(4, arrivals.size)
        assertEquals(false, arrivals[0].isRealtime)
        // Service-day ordering: 23:30, 23:50 come before 00:30, 01:00
        assertEquals(LocalTime.parse("23:30:00"), arrivals[0].time)
        assertEquals(LocalTime.parse("23:50:00"), arrivals[1].time)
        assertEquals(LocalTime.parse("00:30:00"), arrivals[2].time)
        assertEquals(LocalTime.parse("01:00:00"), arrivals[3].time)
    }

    @Test
    @DisplayName("버스 도착 정보 배치 조회 - 자정 이후 버스 정렬 순서 검증")
    fun testGetArrivalBatchAfterMidnightSorting() {
        val key =
            BusArrivalKey(
                routeID = 216000068,
                stopID = 216000138,
                startStopID = 216000358,
                limit = null,
            )
        whenever(
            realtimeRepository.findByRouteIDInAndStopIDIn(any(), any()),
        ).thenReturn(emptyList())
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndWeekdayAndDepartureTimeAfter(
                any(),
                any(),
                any(),
                any(),
                any(),
            ),
        ).thenReturn(
            listOf(
                BusTimetable(
                    seq = 1,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("22:00:00"),
                ),
            ),
        )
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndWeekdayAndDepartureTimeBefore(
                any(),
                any(),
                any(),
                any(),
                any(),
            ),
        ).thenReturn(
            listOf(
                BusTimetable(
                    seq = 2,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("01:00:00"),
                ),
            ),
        )

        val result = realtimeService.getArrivalBatch(setOf(key))
        val arrivals = result[key]!!

        assertEquals(2, arrivals.size)
        // 22:00 comes before 01:00 in service-day order (01:00 = next-day continuation)
        assertEquals(LocalTime.parse("22:00:00"), arrivals[0].time)
        assertEquals(LocalTime.parse("01:00:00"), arrivals[1].time)
    }

    @Test
    @DisplayName("버스 도착 정보 배치 조회 - limit 적용 시 자정 이후 버스 포함")
    fun testGetArrivalBatchLimitWithAfterMidnightBuses() {
        val key =
            BusArrivalKey(
                routeID = 216000068,
                stopID = 216000138,
                startStopID = 216000358,
                limit = 3,
            )
        whenever(
            realtimeRepository.findByRouteIDInAndStopIDIn(any(), any()),
        ).thenReturn(emptyList())
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndWeekdayAndDepartureTimeAfter(
                any(),
                any(),
                any(),
                any(),
                any(),
            ),
        ).thenReturn(
            listOf(
                BusTimetable(
                    seq = 1,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("23:00:00"),
                ),
                BusTimetable(
                    seq = 2,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("23:30:00"),
                ),
            ),
        )
        whenever(
            timetableRepository.findByRouteIDInAndStartStopIDInAndWeekdayAndDepartureTimeBefore(
                any(),
                any(),
                any(),
                any(),
                any(),
            ),
        ).thenReturn(
            listOf(
                BusTimetable(
                    seq = 3,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime =
                        LocalTime.parse("00:30:00"),
                ),
                BusTimetable(
                    seq = 4,
                    routeID = 216000068,
                    startStopID = 216000358,
                    weekday = "weekdays",
                    departureTime = LocalTime.parse("01:00:00"),
                ),
            ),
        )

        val result = realtimeService.getArrivalBatch(setOf(key))
        val arrivals = result[key]!!

        // limit = 3: 23:00, 23:30, 00:30 (01:00 cut off)
        assertEquals(3, arrivals.size)
        assertEquals(LocalTime.parse("23:00:00"), arrivals[0].time)
        assertEquals(LocalTime.parse("23:30:00"), arrivals[1].time)
        assertEquals(LocalTime.parse("00:30:00"), arrivals[2].time)
    }
}
