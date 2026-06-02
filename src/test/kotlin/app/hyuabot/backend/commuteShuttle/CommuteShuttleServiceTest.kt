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
import app.hyuabot.backend.database.repository.CommuteShuttleRouteRepository
import app.hyuabot.backend.database.repository.CommuteShuttleStopRepository
import app.hyuabot.backend.database.repository.CommuteShuttleTimetableRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argThat
import org.mockito.kotlin.whenever
import java.time.LocalTime
import java.util.Optional
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class CommuteShuttleServiceTest {
    @Mock
    private lateinit var routeRepository: CommuteShuttleRouteRepository

    @Mock
    private lateinit var stopRepository: CommuteShuttleStopRepository

    @Mock
    private lateinit var timetableRepository: CommuteShuttleTimetableRepository

    @InjectMocks
    private lateinit var service: CommuteShuttleService

    @Test
    @DisplayName("통학버스 노선 목록 조회")
    fun testGetAllRoutes() {
        whenever(routeRepository.findAll()).thenReturn(
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
        )
        val routes = service.getAllRoutes()
        assertEquals(2, routes.size)
        assertEquals("TEST_ROUTE_1", routes[0].name)
        assertEquals("TEST_ROUTE_2", routes[1].name)
    }

    @Test
    @DisplayName("통학버스 노선 이름으로 조회")
    fun testGetRouteByName() {
        val route =
            CommuteShuttleRoute(
                name = "TEST_ROUTE_1",
                descriptionKorean = "테스트 노선 1",
                descriptionEnglish = "Test Route 1",
                timetable = mutableListOf(),
            )
        whenever(routeRepository.findByName("TEST_ROUTE_1")).thenReturn(route)
        val foundRoute = service.getRouteByName("TEST_ROUTE_1")
        assertEquals("TEST_ROUTE_1", foundRoute.name)
        assertEquals("테스트 노선 1", foundRoute.descriptionKorean)
        assertEquals("Test Route 1", foundRoute.descriptionEnglish)
    }

    @Test
    @DisplayName("통학버스 노선 이름으로 조회 - 존재하지 않는 노선")
    fun testGetRouteByNameNotFound() {
        whenever(routeRepository.findByName("NON_EXISTENT_ROUTE")).thenReturn(null)
        assertThrows<ShuttleRouteNotFoundException> { service.getRouteByName("NON_EXISTENT_ROUTE") }
    }

    @Test
    @DisplayName("통학버스 노선 생성")
    fun testCreateRoute() {
        val newRoute =
            CommuteShuttleRoute(
                name = "NEW_ROUTE",
                descriptionKorean = "새로운 노선",
                descriptionEnglish = "New Route",
                timetable = mutableListOf(),
            )
        whenever(routeRepository.findByName("NEW_ROUTE")).thenReturn(null)
        whenever(routeRepository.save(newRoute)).thenReturn(newRoute)
        val createdRoute =
            service.createRoute(
                CreateShuttleRouteRequest(
                    name = "NEW_ROUTE",
                    descriptionKorean = "새로운 노선",
                    descriptionEnglish = "New Route",
                ),
            )
        assertEquals("NEW_ROUTE", createdRoute.name)
        assertEquals("새로운 노선", createdRoute.descriptionKorean)
        assertEquals("New Route", createdRoute.descriptionEnglish)
    }

    @Test
    @DisplayName("통학버스 노선 생성 - 중복된 이름")
    fun testCreateRouteDuplicateName() {
        val existingRoute =
            CommuteShuttleRoute(
                name = "EXISTING_ROUTE",
                descriptionKorean = "기존 노선",
                descriptionEnglish = "Existing Route",
                timetable = mutableListOf(),
            )
        whenever(routeRepository.findByName("EXISTING_ROUTE")).thenReturn(existingRoute)
        assertThrows<DuplicateShuttleRouteException> {
            service.createRoute(
                CreateShuttleRouteRequest(
                    name = "EXISTING_ROUTE",
                    descriptionKorean = "기존 노선",
                    descriptionEnglish = "Existing Route",
                ),
            )
        }
    }

    @Test
    @DisplayName("통학버스 노선 수정")
    fun testUpdateRoute() {
        val existingRoute =
            CommuteShuttleRoute(
                name = "EXISTING_ROUTE",
                descriptionKorean = "기존 노선",
                descriptionEnglish = "Existing Route",
                timetable = mutableListOf(),
            )
        val updatedRoute =
            CommuteShuttleRoute(
                name = "EXISTING_ROUTE",
                descriptionKorean = "수정된 노선",
                descriptionEnglish = "Updated Route",
                timetable = mutableListOf(),
            )
        whenever(routeRepository.findById("EXISTING_ROUTE")).thenReturn(Optional.of(existingRoute))
        whenever(routeRepository.save(existingRoute)).thenReturn(updatedRoute)
        val result =
            service.updateRoute(
                "EXISTING_ROUTE",
                UpdateShuttleRouteRequest(
                    descriptionKorean = "수정된 노선",
                    descriptionEnglish = "Updated Route",
                ),
            )
        assertEquals("EXISTING_ROUTE", result.name)
        assertEquals("수정된 노선", result.descriptionKorean)
        assertEquals("Updated Route", result.descriptionEnglish)
    }

    @Test
    @DisplayName("통학버스 노선 수정 - 존재하지 않는 노선")
    fun testUpdateRouteNotFound() {
        whenever(routeRepository.findById("NON_EXISTENT_ROUTE")).thenReturn(Optional.empty())
        assertThrows<ShuttleRouteNotFoundException> {
            service.updateRoute(
                "NON_EXISTENT_ROUTE",
                UpdateShuttleRouteRequest(
                    descriptionKorean = "수정된 노선",
                    descriptionEnglish = "Updated Route",
                ),
            )
        }
    }

    @Test
    @DisplayName("통학버스 노선 삭제")
    fun testDeleteRoute() {
        val existingRoute =
            CommuteShuttleRoute(
                name = "EXISTING_ROUTE",
                descriptionKorean = "기존 노선",
                descriptionEnglish = "Existing Route",
                timetable = mutableListOf(),
            )
        whenever(routeRepository.findById("EXISTING_ROUTE")).thenReturn(Optional.of(existingRoute))
        whenever(timetableRepository.findByRouteName("EXISTING_ROUTE")).thenReturn(emptyList())
        service.deleteRoute("EXISTING_ROUTE")
    }

    @Test
    @DisplayName("통학버스 노선 삭제 - 존재하지 않는 노선")
    fun testDeleteRouteNotFound() {
        whenever(routeRepository.findById("NON_EXISTENT_ROUTE")).thenReturn(Optional.empty())
        assertThrows<ShuttleRouteNotFoundException> { service.deleteRoute("NON_EXISTENT_ROUTE") }
    }

    @Test
    @DisplayName("통학버스 정류장 목록 조회")
    fun testGetAllStops() {
        whenever(stopRepository.findAll()).thenReturn(
            listOf(
                CommuteShuttleStop(
                    name = "STOP_1",
                    description = "정류장 1",
                    latitude = 37.123456,
                    longitude = 127.123456,
                    timetable = mutableListOf(),
                ),
                CommuteShuttleStop(
                    name = "STOP_2",
                    description = "정류장 2",
                    latitude = 37.654321,
                    longitude = 127.654321,
                    timetable = mutableListOf(),
                ),
            ),
        )
        val stops = service.getAllStops()
        assertEquals(2, stops.size)
        assertEquals("STOP_1", stops[0].name)
        assertEquals("STOP_2", stops[1].name)
    }

    @Test
    @DisplayName("통학버스 정류장 이름으로 조회")
    fun testGetStopByName() {
        val stop =
            CommuteShuttleStop(
                name = "STOP_1",
                description = "정류장 1",
                latitude = 37.123456,
                longitude = 127.123456,
                timetable = mutableListOf(),
            )
        whenever(stopRepository.findByName("STOP_1")).thenReturn(stop)
        val foundStop = service.getStopByName("STOP_1")
        assertEquals("STOP_1", foundStop.name)
        assertEquals("정류장 1", foundStop.description)
        assertEquals(37.123456, foundStop.latitude)
        assertEquals(127.123456, foundStop.longitude)
    }

    @Test
    @DisplayName("통학버스 정류장 이름으로 조회 - 존재하지 않는 정류장")
    fun testGetStopByNameNotFound() {
        whenever(stopRepository.findByName("NON_EXISTENT_STOP")).thenReturn(null)
        assertThrows<ShuttleStopNotFoundException> { service.getStopByName("NON_EXISTENT_STOP") }
    }

    @Test
    @DisplayName("통학버스 정류장 생성")
    fun testCreateStop() {
        val newStop =
            CommuteShuttleStop(
                name = "NEW_STOP",
                description = "새로운 정류장",
                latitude = 37.123456,
                longitude = 127.123456,
                timetable = mutableListOf(),
            )
        whenever(stopRepository.findByName("NEW_STOP")).thenReturn(null)
        whenever(stopRepository.save(newStop)).thenReturn(newStop)
        val createdStop =
            service.createStop(
                CreateShuttleStopRequest(
                    name = "NEW_STOP",
                    description = "새로운 정류장",
                    latitude = 37.123456,
                    longitude = 127.123456,
                ),
            )
        assertEquals("NEW_STOP", createdStop.name)
        assertEquals("새로운 정류장", createdStop.description)
        assertEquals(37.123456, createdStop.latitude)
        assertEquals(127.123456, createdStop.longitude)
    }

    @Test
    @DisplayName("통학버스 정류장 생성 - 중복된 이름")
    fun testCreateStopDuplicateName() {
        val existingStop =
            CommuteShuttleStop(
                name = "EXISTING_STOP",
                description = "기존 정류장",
                latitude = 37.123456,
                longitude = 127.123456,
                timetable = mutableListOf(),
            )
        whenever(stopRepository.findByName("EXISTING_STOP")).thenReturn(existingStop)
        assertThrows<DuplicateShuttleStopException> {
            service.createStop(
                CreateShuttleStopRequest(
                    name = "EXISTING_STOP",
                    description = "기존 정류장",
                    latitude = 37.123456,
                    longitude = 127.123456,
                ),
            )
        }
    }

    @Test
    @DisplayName("통학버스 정류장 수정")
    fun testUpdateStop() {
        val existingStop =
            CommuteShuttleStop(
                name = "EXISTING_STOP",
                description = "기존 정류장",
                latitude = 37.123456,
                longitude = 127.123456,
                timetable = mutableListOf(),
            )
        val updatedStop =
            CommuteShuttleStop(
                name = "EXISTING_STOP",
                description = "수정된 정류장",
                latitude = 37.654321,
                longitude = 127.654321,
                timetable = mutableListOf(),
            )
        whenever(stopRepository.findById("EXISTING_STOP")).thenReturn(Optional.of(existingStop))
        whenever(stopRepository.save(existingStop)).thenReturn(updatedStop)
        val result =
            service.updateStop(
                "EXISTING_STOP",
                UpdateShuttleStopRequest(
                    description = "수정된 정류장",
                    latitude = 37.654321,
                    longitude = 127.654321,
                ),
            )
        assertEquals("EXISTING_STOP", result.name)
        assertEquals("수정된 정류장", result.description)
        assertEquals(37.654321, result.latitude)
        assertEquals(127.654321, result.longitude)
    }

    @Test
    @DisplayName("통학버스 정류장 수정 - 존재하지 않는 정류장")
    fun testUpdateStopNotFound() {
        whenever(stopRepository.findById("NON_EXISTENT_STOP")).thenReturn(Optional.empty())
        assertThrows<ShuttleStopNotFoundException> {
            service.updateStop(
                "NON_EXISTENT_STOP",
                UpdateShuttleStopRequest(
                    description = "수정된 정류장",
                    latitude = 37.654321,
                    longitude = 127.654321,
                ),
            )
        }
    }

    @Test
    @DisplayName("통학버스 정류장 삭제")
    fun testDeleteStop() {
        val existingStop =
            CommuteShuttleStop(
                name = "EXISTING_STOP",
                description = "기존 정류장",
                latitude = 37.123456,
                longitude = 127.123456,
                timetable = mutableListOf(),
            )
        whenever(stopRepository.findById("EXISTING_STOP")).thenReturn(Optional.of(existingStop))
        whenever(timetableRepository.findByStopName("EXISTING_STOP")).thenReturn(emptyList())
        service.deleteStop("EXISTING_STOP")
    }

    @Test
    @DisplayName("통학버스 정류장 삭제 - 존재하지 않는 정류장")
    fun testDeleteStopNotFound() {
        whenever(stopRepository.findById("NON_EXISTENT_STOP")).thenReturn(Optional.empty())
        assertThrows<ShuttleStopNotFoundException> { service.deleteStop("NON_EXISTENT_STOP") }
    }

    @Test
    @DisplayName("통학버스 시간표 조회")
    fun testGetTimetableByRouteName() {
        val timetables =
            listOf(
                CommuteShuttleTimetable(
                    routeName = "TEST_ROUTE",
                    stopName = "STOP_1",
                    order = 0,
                    departureTime = LocalTime.parse("16:00"),
                    route = null,
                    stop = null,
                ),
                CommuteShuttleTimetable(
                    routeName = "TEST_ROUTE",
                    stopName = "STOP_2",
                    order = 1,
                    departureTime = LocalTime.parse("16:10"),
                    route = null,
                    stop = null,
                ),
            )
        whenever(timetableRepository.findAll()).thenReturn(timetables)
        val result = service.getAllTimetables()
        assertEquals(2, result.size)
        assertEquals("STOP_1", result[0].stopName)
        assertEquals("STOP_2", result[1].stopName)
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 목록 조회")
    fun testGetTimetableByRouteNameOnly() {
        val timetables =
            listOf(
                CommuteShuttleTimetable(
                    routeName = "TEST_ROUTE",
                    stopName = "STOP_1",
                    order = 0,
                    departureTime = LocalTime.parse("16:00"),
                    route = null,
                    stop = null,
                ),
                CommuteShuttleTimetable(
                    routeName = "TEST_ROUTE",
                    stopName = "STOP_2",
                    order = 1,
                    departureTime = LocalTime.parse("16:10"),
                    route = null,
                    stop = null,
                ),
            )
        whenever(timetableRepository.findByRouteName("TEST_ROUTE")).thenReturn(timetables)
        val result = service.getTimetableByRouteName("TEST_ROUTE")
        assertEquals(2, result.size)
        assertEquals("STOP_1", result[0].stopName)
        assertEquals("STOP_2", result[1].stopName)
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 항목 조회")
    fun testGetShuttleTimetableByRouteNameAndSeq() {
        val timetable =
            CommuteShuttleTimetable(
                routeName = "TEST_ROUTE",
                stopName = "STOP_1",
                order = 0,
                departureTime = LocalTime.parse("16:00"),
                route = null,
                stop = null,
            )
        whenever(timetableRepository.findByRouteNameAndSeq("TEST_ROUTE", 0)).thenReturn(timetable)
        val result = service.getShuttleTimetableByRouteNameAndSeq("TEST_ROUTE", 0)
        assertEquals("TEST_ROUTE", result.routeName)
        assertEquals("STOP_1", result.stopName)
        assertEquals(0, result.order)
        assertEquals(LocalTime.parse("16:00"), result.departureTime)
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 항목 조회 - 존재하지 않는 항목")
    fun testGetShuttleTimetableByRouteNameAndSeqNotFound() {
        whenever(timetableRepository.findByRouteNameAndSeq("TEST_ROUTE", 99)).thenReturn(null)
        assertThrows<ShuttleTimetableNotFoundException> { service.getShuttleTimetableByRouteNameAndSeq("TEST_ROUTE", 99) }
    }

    @Test
    @DisplayName("통학버스 시간표 생성")
    fun testCreateTimetable() {
        val newTimetable =
            CommuteShuttleTimetable(
                routeName = "TEST_ROUTE",
                stopName = "STOP_1",
                order = 0,
                departureTime = LocalTime.parse("16:00"),
                route = null,
                stop = null,
            )
        whenever(routeRepository.findById("TEST_ROUTE")).thenReturn(
            Optional.of(
                CommuteShuttleRoute(
                    "TEST_ROUTE",
                    "descK",
                    "descE",
                    mutableListOf(),
                ),
            ),
        )
        whenever(stopRepository.findById("STOP_1")).thenReturn(
            Optional.of(
                CommuteShuttleStop(
                    "STOP_1",
                    "desc",
                    37.0,
                    127.0,
                    mutableListOf(),
                ),
            ),
        )
        whenever(
            timetableRepository.save(
                argThat<CommuteShuttleTimetable> {
                    routeName == "TEST_ROUTE" &&
                        stopName == "STOP_1" &&
                        order == 0 &&
                        departureTime == LocalTime.parse("16:00")
                },
            ),
        ).thenReturn(newTimetable)
        val createdTimetable =
            service.createTimetable(
                "TEST_ROUTE",
                ShuttleTimetableRequest(
                    routeName = "TEST_ROUTE",
                    stopID = "STOP_1",
                    order = 0,
                    time = "16:00:00",
                ),
            )
        assertEquals("TEST_ROUTE", createdTimetable.routeName)
        assertEquals("STOP_1", createdTimetable.stopName)
        assertEquals(0, createdTimetable.order)
        assertEquals(LocalTime.parse("16:00"), createdTimetable.departureTime)
    }

    @Test
    @DisplayName("통학버스 시간표 생성 - 존재하지 않는 노선")
    fun testCreateTimetableRouteNotFound() {
        whenever(routeRepository.findById("NON_EXISTENT_ROUTE")).thenReturn(Optional.empty())
        assertThrows<ShuttleRouteNotFoundException> {
            service.createTimetable(
                "NON_EXISTENT_ROUTE",
                ShuttleTimetableRequest(
                    routeName = "NON_EXISTENT_ROUTE",
                    stopID = "STOP_1",
                    order = 0,
                    time = "16:00:00",
                ),
            )
        }
    }

    @Test
    @DisplayName("통학버스 시간표 생성 - 존재하지 않는 정류장")
    fun testCreateTimetableStopNotFound() {
        whenever(routeRepository.findById("TEST_ROUTE")).thenReturn(
            Optional.of(
                CommuteShuttleRoute(
                    "TEST_ROUTE",
                    "descK",
                    "descE",
                    mutableListOf(),
                ),
            ),
        )
        whenever(stopRepository.findById("NON_EXISTENT_STOP")).thenReturn(Optional.empty())
        assertThrows<ShuttleStopNotFoundException> {
            service.createTimetable(
                "TEST_ROUTE",
                ShuttleTimetableRequest(
                    routeName = "TEST_ROUTE",
                    stopID = "NON_EXISTENT_STOP",
                    order = 0,
                    time = "16:00:00",
                ),
            )
        }
    }

    @Test
    @DisplayName("통학버스 시간표 생성 - 잘못된 시간 형식")
    fun testCreateTimetableInvalidTimeFormat() {
        whenever(routeRepository.findById("TEST_ROUTE")).thenReturn(
            Optional.of(
                CommuteShuttleRoute(
                    "TEST_ROUTE",
                    "descK",
                    "descE",
                    mutableListOf(),
                ),
            ),
        )
        whenever(stopRepository.findById("STOP_1")).thenReturn(
            Optional.of(
                CommuteShuttleStop(
                    "STOP_1",
                    "desc",
                    37.0,
                    127.0,
                    mutableListOf(),
                ),
            ),
        )
        assertThrows<LocalTimeNotValidException> {
            service.createTimetable(
                "TEST_ROUTE",
                ShuttleTimetableRequest(
                    routeName = "TEST_ROUTE",
                    stopID = "STOP_1",
                    order = 0,
                    time = "invalid_time_format",
                ),
            )
        }
    }

    @Test
    @DisplayName("통학버스 시간표 수정")
    fun testUpdateTimetable() {
        val existingTimetable =
            CommuteShuttleTimetable(
                routeName = "TEST_ROUTE",
                stopName = "STOP_1",
                order = 0,
                departureTime = LocalTime.parse("16:00"),
                route = null,
                stop = null,
            )
        val updatedTimetable =
            CommuteShuttleTimetable(
                routeName = "TEST_ROUTE",
                stopName = "STOP_2",
                order = 1,
                departureTime = LocalTime.parse("16:10"),
                route = null,
                stop = null,
            )
        whenever(timetableRepository.findById(0)).thenReturn(Optional.of(existingTimetable))
        whenever(routeRepository.findById("TEST_ROUTE")).thenReturn(
            Optional.of(
                CommuteShuttleRoute(
                    "TEST_ROUTE",
                    "descK",
                    "descE",
                    mutableListOf(),
                ),
            ),
        )
        whenever(stopRepository.findById("STOP_2")).thenReturn(
            Optional.of(
                CommuteShuttleStop(
                    "STOP_2",
                    "desc",
                    37.0,
                    127.0,
                    mutableListOf(),
                ),
            ),
        )
        whenever(timetableRepository.save(existingTimetable)).thenReturn(updatedTimetable)
        val result =
            service.updateTimetable(
                "TEST_ROUTE",
                0,
                ShuttleTimetableRequest(
                    routeName = "TEST_ROUTE",
                    stopID = "STOP_2",
                    order = 1,
                    time = "16:10:00",
                ),
            )
        assertEquals("TEST_ROUTE", result.routeName)
        assertEquals("STOP_2", result.stopName)
        assertEquals(1, result.order)
        assertEquals(LocalTime.parse("16:10"), result.departureTime)
    }

    @Test
    @DisplayName("통학버스 시간표 수정 - 존재하지 않는 항목")
    fun testUpdateTimetableNotFound() {
        whenever(timetableRepository.findById(99)).thenReturn(Optional.empty())
        assertThrows<ShuttleTimetableNotFoundException> {
            service.updateTimetable(
                "TEST_ROUTE",
                99,
                ShuttleTimetableRequest(
                    routeName = "TEST_ROUTE",
                    stopID = "STOP_2",
                    order = 1,
                    time = "16:10:00",
                ),
            )
        }
    }

    @Test
    @DisplayName("통학버스 시간표 수정 - 존재하지 않는 노선")
    fun testUpdateTimetableRouteNotFound() {
        val existingTimetable =
            CommuteShuttleTimetable(
                routeName = "TEST_ROUTE",
                stopName = "STOP_1",
                order = 0,
                departureTime = LocalTime.parse("16:00"),
                route = null,
                stop = null,
            )
        whenever(timetableRepository.findById(0)).thenReturn(Optional.of(existingTimetable))
        whenever(routeRepository.findById("NON_EXISTENT_ROUTE")).thenReturn(Optional.empty())
        assertThrows<ShuttleRouteNotFoundException> {
            service.updateTimetable(
                "NON_EXISTENT_ROUTE",
                0,
                ShuttleTimetableRequest(
                    routeName = "NON_EXISTENT_ROUTE",
                    stopID = "STOP_2",
                    order = 1,
                    time = "16:10:00",
                ),
            )
        }
    }

    @Test
    @DisplayName("통학버스 시간표 수정 - 존재하지 않는 정류장")
    fun testUpdateTimetableStopNotFound() {
        val existingTimetable =
            CommuteShuttleTimetable(
                routeName = "TEST_ROUTE",
                stopName = "STOP_1",
                order = 0,
                departureTime = LocalTime.parse("16:00"),
                route = null,
                stop = null,
            )
        whenever(timetableRepository.findById(0)).thenReturn(Optional.of(existingTimetable))
        whenever(routeRepository.findById("TEST_ROUTE")).thenReturn(
            Optional.of(
                CommuteShuttleRoute(
                    "TEST_ROUTE",
                    "descK",
                    "descE",
                    mutableListOf(),
                ),
            ),
        )
        whenever(stopRepository.findById("NON_EXISTENT_STOP")).thenReturn(Optional.empty())
        assertThrows<ShuttleStopNotFoundException> {
            service.updateTimetable(
                "TEST_ROUTE",
                0,
                ShuttleTimetableRequest(
                    routeName = "TEST_ROUTE",
                    stopID = "NON_EXISTENT_STOP",
                    order = 1,
                    time = "16:10:00",
                ),
            )
        }
    }

    @Test
    @DisplayName("통학버스 시간표 수정 - 잘못된 시간 형식")
    fun testUpdateTimetableInvalidTimeFormat() {
        val existingTimetable =
            CommuteShuttleTimetable(
                routeName = "TEST_ROUTE",
                stopName = "STOP_1",
                order = 0,
                departureTime = LocalTime.parse("16:00"),
                route = null,
                stop = null,
            )
        whenever(timetableRepository.findById(0)).thenReturn(Optional.of(existingTimetable))
        whenever(routeRepository.findById("TEST_ROUTE")).thenReturn(
            Optional.of(
                CommuteShuttleRoute(
                    "TEST_ROUTE",
                    "descK",
                    "descE",
                    mutableListOf(),
                ),
            ),
        )
        whenever(stopRepository.findById("STOP_2")).thenReturn(
            Optional.of(
                CommuteShuttleStop(
                    "STOP_2",
                    "desc",
                    37.0,
                    127.0,
                    mutableListOf(),
                ),
            ),
        )
        assertThrows<LocalTimeNotValidException> {
            service.updateTimetable(
                "TEST_ROUTE",
                0,
                ShuttleTimetableRequest(
                    routeName = "TEST_ROUTE",
                    stopID = "STOP_2",
                    order = 1,
                    time = "invalid_time_format",
                ),
            )
        }
    }

    @Test
    @DisplayName("통학버스 시간표 삭제")
    fun testDeleteTimetable() {
        val existingTimetable =
            CommuteShuttleTimetable(
                routeName = "TEST_ROUTE",
                stopName = "STOP_1",
                order = 0,
                departureTime = LocalTime.parse("16:00"),
                route = null,
                stop = null,
            )
        whenever(timetableRepository.findById(0)).thenReturn(Optional.of(existingTimetable))
        service.deleteTimetable(0)
    }

    @Test
    @DisplayName("통학버스 시간표 삭제 - 존재하지 않는 항목")
    fun testDeleteTimetableNotFound() {
        whenever(timetableRepository.findById(99)).thenReturn(Optional.empty())
        assertThrows<ShuttleTimetableNotFoundException> { service.deleteTimetable(99) }
    }
}
