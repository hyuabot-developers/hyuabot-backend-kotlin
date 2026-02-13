package app.hyuabot.backend.subway

import app.hyuabot.backend.database.entity.SubwayRealtime
import app.hyuabot.backend.database.entity.SubwayRoute
import app.hyuabot.backend.database.entity.SubwayRouteStation
import app.hyuabot.backend.database.entity.SubwayStation
import app.hyuabot.backend.database.entity.SubwayTimetable
import app.hyuabot.backend.database.exception.DurationNotValidException
import app.hyuabot.backend.database.exception.LocalTimeNotValidException
import app.hyuabot.backend.database.repository.SubwayRealtimeRepository
import app.hyuabot.backend.database.repository.SubwayRouteRepository
import app.hyuabot.backend.database.repository.SubwayStationNameRepository
import app.hyuabot.backend.database.repository.SubwayStationRepository
import app.hyuabot.backend.database.repository.SubwayTimetableRepository
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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.Optional
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class SubwayServiceTest {
    @Mock private lateinit var nameRepository: SubwayStationNameRepository

    @Mock private lateinit var stationRepository: SubwayStationRepository

    @Mock private lateinit var routeRepository: SubwayRouteRepository

    @Mock private lateinit var timetableRepository: SubwayTimetableRepository

    @Mock private lateinit var realtimeRepository: SubwayRealtimeRepository

    @InjectMocks private lateinit var service: SubwayService

    @Test
    @DisplayName("지하철 노선 목록 조회")
    fun testGetSubwayRoutes() {
        whenever(routeRepository.findAll()).thenReturn(
            listOf(
                SubwayRoute(
                    id = 1,
                    name = "1호선",
                    station = listOf(),
                ),
                SubwayRoute(
                    id = 2,
                    name = "2호선",
                    station = listOf(),
                ),
            ),
        )
        val routes = service.getSubwayRoutes()
        assertEquals(2, routes.size)
        assertEquals("1호선", routes[0].name)
        assertEquals("2호선", routes[1].name)
    }

    @Test
    @DisplayName("지하철 노선 생성")
    fun testCreateSubwayRoute() {
        val newRoute =
            SubwayRoute(
                id = 3,
                name = "3호선",
                station = listOf(),
            )
        whenever(routeRepository.findById(3)).thenReturn(Optional.empty())
        whenever(routeRepository.save(newRoute)).thenReturn(newRoute)
        val createdRoute =
            service.createSubwayRoute(
                CreateSubwayRouteRequest(
                    id = 3,
                    name = "3호선",
                ),
            )
        assertEquals(3, createdRoute.id)
        assertEquals("3호선", createdRoute.name)
    }

    @Test
    @DisplayName("지하철 노선 생성 - 중복된 ID 예외 처리")
    fun testCreateSubwayRouteDuplicateIdException() {
        val existingRoute =
            SubwayRoute(
                id = 1,
                name = "1호선",
                station = listOf(),
            )
        whenever(routeRepository.findById(1)).thenReturn(Optional.of(existingRoute))
        assertThrows<DuplicateSubwayRouteException> {
            service.createSubwayRoute(
                CreateSubwayRouteRequest(
                    id = 1,
                    name = "1호선",
                ),
            )
        }
    }

    @Test
    @DisplayName("지하철 노선 항목 조회")
    fun testGetSubwayRouteById() {
        val route =
            SubwayRoute(
                id = 1,
                name = "1호선",
                station = listOf(),
            )
        whenever(routeRepository.findById(1)).thenReturn(Optional.of(route))
        val foundRoute = service.getSubwayRouteById(1)
        assertEquals(1, foundRoute.id)
        assertEquals("1호선", foundRoute.name)
    }

    @Test
    @DisplayName("지하철 노선 항목 조회 - 없는 ID 예외 처리")
    fun testGetSubwayRouteByIdNotFoundException() {
        whenever(routeRepository.findById(999)).thenReturn(Optional.empty())
        assertThrows<SubwayRouteNotFoundException> {
            service.getSubwayRouteById(999)
        }
    }

    @Test
    @DisplayName("지하철 노선 수정")
    fun testUpdateSubwayRoute() {
        val existingRoute =
            SubwayRoute(
                id = 1,
                name = "1호선",
                station = listOf(),
            )
        val updatedRoute =
            SubwayRoute(
                id = 1,
                name = "1호선 - 수정됨",
                station = listOf(),
            )
        whenever(routeRepository.findById(1)).thenReturn(Optional.of(existingRoute))
        whenever(routeRepository.save(existingRoute)).thenReturn(updatedRoute)
        val result =
            service.updateSubwayRoute(
                1,
                UpdateSubwayRouteRequest(name = "1호선 - 수정됨"),
            )
        assertEquals(1, result.id)
        assertEquals("1호선 - 수정됨", result.name)
    }

    @Test
    @DisplayName("지하철 노선 수정 - 없는 ID 예외 처리")
    fun testUpdateSubwayRouteNotFoundException() {
        whenever(routeRepository.findById(999)).thenReturn(Optional.empty())
        assertThrows<SubwayRouteNotFoundException> {
            service.updateSubwayRoute(
                999,
                UpdateSubwayRouteRequest(name = "없는 노선"),
            )
        }
    }

    @Test
    @DisplayName("지하철 노선 삭제")
    fun testDeleteSubwayRoute() {
        val existingRoute =
            SubwayRoute(
                id = 1,
                name = "1호선",
                station =
                    listOf(
                        SubwayRouteStation(
                            id = "K450",
                            routeID = 1,
                            name = "중앙",
                            order = 50,
                            cumulativeTime = Duration.ofMinutes(5),
                            route = null,
                            stationName = null,
                            realtime = emptyList(),
                            timetable = emptyList(),
                        ),
                        SubwayRouteStation(
                            id = "K251",
                            routeID = 1,
                            name = "강남",
                            order = 2,
                            cumulativeTime = Duration.ofMinutes(3),
                            route = null,
                            stationName = null,
                            realtime = null,
                            timetable = null,
                        ),
                    ),
            )
        whenever(routeRepository.findById(1)).thenReturn(Optional.of(existingRoute))
        service.deleteSubwayRoute(1)
    }

    @Test
    @DisplayName("지하철 노선 삭제 - 없는 ID 예외 처리")
    fun testDeleteSubwayRouteNotFoundException() {
        whenever(routeRepository.findById(999)).thenReturn(Optional.empty())
        assertThrows<SubwayRouteNotFoundException> {
            service.deleteSubwayRoute(999)
        }
    }

    @Test
    @DisplayName("지하철 역 목록 조회")
    fun testGetAllStations() {
        whenever(stationRepository.findAll()).thenReturn(
            listOf(
                SubwayRouteStation(
                    id = "K450",
                    routeID = 1004,
                    name = "중앙",
                    order = 50,
                    cumulativeTime = Duration.ofMinutes(5),
                    route = null,
                    stationName = null,
                    realtime = null,
                    timetable = null,
                ),
                SubwayRouteStation(
                    id = "K251",
                    routeID = 2002,
                    name = "강남",
                    order = 2,
                    cumulativeTime = Duration.ofMinutes(3),
                    route = null,
                    stationName = null,
                    realtime = null,
                    timetable = null,
                ),
            ),
        )
        val stations = service.getAllStations()
        assertEquals(2, stations.size)
        assertEquals("K450", stations[0].id)
        assertEquals("중앙", stations[0].name)
        assertEquals("K251", stations[1].id)
        assertEquals("강남", stations[1].name)
    }

    @Test
    @DisplayName("지하철 역 생성 - 역 이름 중복 없음")
    fun testCreateStation() {
        val newStation =
            SubwayRouteStation(
                id = "K300",
                routeID = 3003,
                name = "신림",
                order = 10,
                cumulativeTime = Duration.ofMinutes(7),
                route = null,
                stationName = null,
                realtime = emptyList(),
                timetable = emptyList(),
            )
        whenever(stationRepository.findById("K300")).thenReturn(Optional.empty())
        whenever(nameRepository.findByName("신림")).thenReturn(null)
        whenever(nameRepository.save(SubwayStation(name = "신림", emptyList()))).thenReturn(
            SubwayStation(name = "신림", emptyList()),
        )
        whenever(stationRepository.save(newStation)).thenReturn(newStation)
        val createdStation =
            service.createStation(
                CreateSubwayStationRequest(
                    id = "K300",
                    routeID = 3003,
                    name = "신림",
                    order = 10,
                    cumulativeTime = "00:07:00",
                ),
            )
        assertEquals("K300", createdStation.id)
        assertEquals("신림", createdStation.name)
        assertEquals(3003, createdStation.routeID)
        assertEquals(10, createdStation.order)
        assertEquals(Duration.ofMinutes(7), createdStation.cumulativeTime)
    }

    @Test
    @DisplayName("지하철 역 생성 - 역 이름 중복 있음")
    fun testCreateStationWithExistingName() {
        val newStation =
            SubwayRouteStation(
                id = "K301",
                routeID = 3003,
                name = "신림",
                order = 11,
                cumulativeTime = Duration.ofMinutes(8),
                route = null,
                stationName = null,
                realtime = emptyList(),
                timetable = emptyList(),
            )
        val existingName =
            SubwayStation(
                name = "신림",
                subwayLine = listOf(),
            )
        whenever(stationRepository.findById("K301")).thenReturn(Optional.empty())
        whenever(nameRepository.findByName("신림")).thenReturn(existingName)
        whenever(stationRepository.save(newStation)).thenReturn(newStation)
        val createdStation =
            service.createStation(
                CreateSubwayStationRequest(
                    id = "K301",
                    routeID = 3003,
                    name = "신림",
                    order = 11,
                    cumulativeTime = "00:08:00",
                ),
            )
        assertEquals("K301", createdStation.id)
        assertEquals("신림", createdStation.name)
        assertEquals(3003, createdStation.routeID)
        assertEquals(11, createdStation.order)
        assertEquals(Duration.ofMinutes(8), createdStation.cumulativeTime)
    }

    @Test
    @DisplayName("지하철 역 생성 - 중복된 ID 예외 처리")
    fun testCreateStationDuplicateIdException() {
        val existingStation =
            SubwayRouteStation(
                id = "K450",
                routeID = 1004,
                name = "중앙",
                order = 50,
                cumulativeTime = Duration.ofMinutes(5),
                route = null,
                stationName = null,
                realtime = null,
                timetable = null,
            )
        whenever(stationRepository.findById("K450")).thenReturn(Optional.of(existingStation))
        assertThrows<DuplicateSubwayStationException> {
            service.createStation(
                CreateSubwayStationRequest(
                    id = "K450",
                    routeID = 1004,
                    name = "중앙",
                    order = 50,
                    cumulativeTime = "00:05:00",
                ),
            )
        }
    }

    @Test
    @DisplayName("지하철 역 생성 - 잘못된 누적 시간 형식 예외 처리")
    fun testCreateStationInvalidDurationFormatException() {
        assertThrows<DurationNotValidException> {
            service.createStation(
                CreateSubwayStationRequest(
                    id = "K302",
                    routeID = 3003,
                    name = "신림",
                    order = 12,
                    cumulativeTime = "invalid_format",
                ),
            )
        }
    }

    @Test
    @DisplayName("지하철 역 항목 조회")
    fun testGetStationById() {
        val station =
            SubwayRouteStation(
                id = "K450",
                routeID = 1004,
                name = "중앙",
                order = 50,
                cumulativeTime = Duration.ofMinutes(5),
                route = null,
                stationName = null,
                realtime = null,
                timetable = null,
            )
        whenever(stationRepository.findById("K450")).thenReturn(Optional.of(station))
        val foundStation = service.getStationById("K450")
        assertEquals("K450", foundStation.id)
        assertEquals("중앙", foundStation.name)
    }

    @Test
    @DisplayName("지하철 역 항목 조회 - 없는 ID 예외 처리")
    fun testGetStationByIdNotFoundException() {
        whenever(stationRepository.findById("INVALID_ID")).thenReturn(Optional.empty())
        assertThrows<SubwayStationNotFoundException> {
            service.getStationById("INVALID_ID")
        }
    }

    @Test
    @DisplayName("지하철 역 수정 - 존재하지 않는 역 이름으로 수정")
    fun testUpdateStation() {
        val existingStation =
            SubwayRouteStation(
                id = "K450",
                routeID = 1004,
                name = "중앙",
                order = 50,
                cumulativeTime = Duration.ofMinutes(5),
                route = null,
                stationName = null,
                realtime = null,
                timetable = null,
            )
        val updatedStation =
            SubwayRouteStation(
                id = "K450",
                routeID = 1004,
                name = "중앙역 - 수정됨",
                order = 51,
                cumulativeTime = Duration.ofMinutes(6),
                route = null,
                stationName = null,
                realtime = null,
                timetable = null,
            )
        whenever(stationRepository.findById("K450")).thenReturn(Optional.of(existingStation))
        whenever(nameRepository.findByName("중앙역 - 수정됨")).thenReturn(null)
        whenever(nameRepository.save(SubwayStation(name = "중앙역 - 수정됨", emptyList()))).thenReturn(
            SubwayStation(name = "중앙역 - 수정됨", emptyList()),
        )
        whenever(stationRepository.save(existingStation)).thenReturn(updatedStation)
        val result =
            service.updateStation(
                "K450",
                UpdateSubwayStationRequest(
                    routeID = 1004,
                    name = "중앙역 - 수정됨",
                    order = 51,
                    cumulativeTime = "00:06:00",
                ),
            )
        assertEquals("K450", result.id)
        assertEquals("중앙역 - 수정됨", result.name)
        assertEquals(1004, result.routeID)
        assertEquals(51, result.order)
        assertEquals(Duration.ofMinutes(6), result.cumulativeTime)
    }

    @Test
    @DisplayName("지하철 역 수정 - 기존 역 이름으로 수정")
    fun testUpdateStationWithExistingName() {
        val existingStation =
            SubwayRouteStation(
                id = "K450",
                routeID = 1004,
                name = "중앙",
                order = 50,
                cumulativeTime = Duration.ofMinutes(5),
                route = null,
                stationName = null,
                realtime = null,
                timetable = null,
            )
        val updatedStation =
            SubwayRouteStation(
                id = "K450",
                routeID = 1004,
                name = "중앙역 - 수정됨",
                order = 51,
                cumulativeTime = Duration.ofMinutes(6),
                route = null,
                stationName = null,
                realtime = null,
                timetable = null,
            )
        whenever(stationRepository.findById("K450")).thenReturn(Optional.of(existingStation))
        whenever(nameRepository.findByName("중앙역 - 수정됨")).thenReturn(
            SubwayStation(name = "중앙역 - 수정됨", subwayLine = listOf()),
        )
        whenever(stationRepository.save(existingStation)).thenReturn(updatedStation)
        val result =
            service.updateStation(
                "K450",
                UpdateSubwayStationRequest(
                    routeID = 1004,
                    name = "중앙역 - 수정됨",
                    order = 51,
                    cumulativeTime = "00:06:00",
                ),
            )
        assertEquals("K450", result.id)
        assertEquals("중앙역 - 수정됨", result.name)
        assertEquals(1004, result.routeID)
        assertEquals(51, result.order)
        assertEquals(Duration.ofMinutes(6), result.cumulativeTime)
    }

    @Test
    @DisplayName("지하철 역 수정 - 없는 ID 예외 처리")
    fun testUpdateStationNotFoundException() {
        whenever(stationRepository.findById("INVALID_ID")).thenReturn(Optional.empty())
        assertThrows<SubwayStationNotFoundException> {
            service.updateStation(
                "INVALID_ID",
                UpdateSubwayStationRequest(
                    routeID = 1004,
                    name = "없는 역",
                    order = 1,
                    cumulativeTime = "00:01:00",
                ),
            )
        }
    }

    @Test
    @DisplayName("지하철 역 수정 - 잘못된 누적 시간 형식 예외 처리")
    fun testUpdateStationInvalidDurationFormatException() {
        val existingStation =
            SubwayRouteStation(
                id = "K450",
                routeID = 1004,
                name = "중앙",
                order = 50,
                cumulativeTime = Duration.ofMinutes(5),
                route = null,
                stationName = null,
                realtime = null,
                timetable = null,
            )
        whenever(stationRepository.findById("K450")).thenReturn(Optional.of(existingStation))
        assertThrows<DurationNotValidException> {
            service.updateStation(
                "K450",
                UpdateSubwayStationRequest(
                    routeID = 1004,
                    name = "중앙",
                    order = 50,
                    cumulativeTime = "invalid_format",
                ),
            )
        }
    }

    @Test
    @DisplayName("지하철 역 삭제 - 역 이름 정리 포함")
    fun testDeleteStation() {
        val existingStation =
            SubwayRouteStation(
                id = "K450",
                routeID = 1004,
                name = "중앙",
                order = 50,
                cumulativeTime = Duration.ofMinutes(5),
                route = null,
                stationName = null,
                realtime = null,
                timetable = null,
            )
        whenever(stationRepository.findById("K450")).thenReturn(Optional.of(existingStation))
        whenever(nameRepository.findByName("중앙")).thenReturn(
            SubwayStation(
                name = "중앙",
                subwayLine = listOf(),
            ),
        )
        service.deleteStation("K450")
        verify(stationRepository).delete(existingStation)
    }

    @Test
    @DisplayName("지하철 역 삭제 - 이름 정리 제외")
    fun testDeleteStationWithoutNameCleanup() {
        val existingStation =
            SubwayRouteStation(
                id = "K450",
                routeID = 1004,
                name = "중앙",
                order = 50,
                cumulativeTime = Duration.ofMinutes(5),
                route = null,
                stationName = null,
                realtime = emptyList(),
                timetable = emptyList(),
            )
        whenever(stationRepository.findById("K450")).thenReturn(Optional.of(existingStation))
        whenever(nameRepository.findByName("중앙")).thenReturn(
            SubwayStation(
                name = "중앙",
                subwayLine =
                    listOf(
                        SubwayRouteStation(
                            id = "K249",
                            routeID = 1071,
                            name = "중앙",
                            order = 49,
                            cumulativeTime = Duration.ofMinutes(5),
                            route = null,
                            stationName = null,
                            realtime = emptyList(),
                            timetable = emptyList(),
                        ),
                    ),
            ),
        )
        service.deleteStation("K450")
        verify(stationRepository).delete(existingStation)
    }

    @Test
    @DisplayName("지하철 역 삭제 - 없는 ID 예외 처리")
    fun testDeleteStationNotFoundException() {
        whenever(stationRepository.findById("INVALID_ID")).thenReturn(Optional.empty())
        assertThrows<SubwayStationNotFoundException> {
            service.deleteStation("INVALID_ID")
        }
    }

    @Test
    @DisplayName("지하철 시간표 목록 조회")
    fun testGetAllTimetables() {
        whenever(timetableRepository.findAll()).thenReturn(
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
                    stationID = "K251",
                    startStationID = "K201",
                    terminalStationID = "K256",
                    departureTime = LocalTime.parse("10:00"),
                    weekday = "weekends",
                    heading = "down",
                    station = null,
                    startStation = null,
                    terminalStation = null,
                ),
            ),
        )
        val timetables = service.getAllTimetables()
        assertEquals(2, timetables.size)
        assertEquals(1, timetables[0].seq)
        assertEquals("K450", timetables[0].stationID)
        assertEquals(2, timetables[1].seq)
        assertEquals("K251", timetables[1].stationID)
    }

    @Test
    @DisplayName("지하철 시간표 목록 조회 - 역 ID로 필터링")
    fun testGetTimetablesByStationID() {
        whenever(timetableRepository.findByStationID("K450")).thenReturn(
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
        )
        val timetables = service.getTimetablesByStationID("K450")
        assertEquals(1, timetables.size)
        assertEquals(1, timetables[0].seq)
        assertEquals("K450", timetables[0].stationID)
    }

    @Test
    @DisplayName("지하철 시간표 목록 조회 - 역 ID 및 행선 필터링")
    fun testGetTimetablesByStationIDAndHeading() {
        whenever(timetableRepository.findByStationIDAndHeading("K450", "up")).thenReturn(
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
        )
        val timetables = service.getTimetablesByStationIDAndDirection("K450", "up")
        assertEquals(1, timetables.size)
        assertEquals(1, timetables[0].seq)
        assertEquals("K450", timetables[0].stationID)
    }

    @Test
    @DisplayName("지하철 시간표 목록 조회 - 역 ID 및 요일 필터링")
    fun testGetTimetablesByStationIDAndWeekday() {
        whenever(timetableRepository.findByStationIDAndWeekday("K450", "weekdays")).thenReturn(
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
        )
        val timetables = service.getTimetablesByStationIDAndWeekday("K450", "weekdays")
        assertEquals(1, timetables.size)
        assertEquals(1, timetables[0].seq)
        assertEquals("K450", timetables[0].stationID)
    }

    @Test
    @DisplayName("지하철 시간표 목록 조회 - 역 ID, 행선 및 요일 필터링")
    fun testGetTimetablesByStationIDAndHeadingAndWeekday() {
        whenever(timetableRepository.findByStationIDAndHeadingAndWeekday("K450", "up", "weekdays")).thenReturn(
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
        )
        val timetables = service.getTimetablesByStationIDAndDirectionAndWeekday("K450", "up", "weekdays")
        assertEquals(1, timetables.size)
        assertEquals(1, timetables[0].seq)
        assertEquals("K450", timetables[0].stationID)
    }

    @Test
    @DisplayName("지하철 시간표 항목 조회")
    fun testGetTimetableByStationIDAndSeq() {
        val timetable =
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
        whenever(timetableRepository.findById(1)).thenReturn(Optional.of(timetable))
        val foundTimetable = service.getTimetableByStationIDAndSeq("K450", 1)
        assertEquals(1, foundTimetable.seq)
        assertEquals("K450", foundTimetable.stationID)
    }

    @Test
    @DisplayName("지하철 시간표 항목 조회 - 없는 ID 예외 처리")
    fun testGetTimetableByStationIDAndSeqNotFoundException() {
        whenever(timetableRepository.findById(999)).thenReturn(Optional.empty())
        assertThrows<SubwayTimetableNotFoundException> {
            service.getTimetableByStationIDAndSeq("K450", 999)
        }
    }

    @Test
    @DisplayName("지하철 시간표 항목 조회 - 역 ID 불일치 예외 처리")
    fun testGetTimetableByStationIDAndSeqStationIDMismatchException() {
        val timetable =
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
        whenever(timetableRepository.findById(1)).thenReturn(Optional.of(timetable))
        assertThrows<SubwayTimetableNotFoundException> {
            service.getTimetableByStationIDAndSeq("K251", 1)
        }
    }

    @Test
    @DisplayName("지하철 시간표 생성")
    fun testCreateTimetable() {
        val newTimetable =
            SubwayTimetable(
                seq = 3,
                stationID = "K450",
                startStationID = "K410",
                terminalStationID = "K456",
                departureTime = LocalTime.parse("11:00"),
                weekday = "weekdays",
                heading = "up",
                station = null,
                startStation = null,
                terminalStation = null,
            )
        whenever(stationRepository.findById("K450")).thenReturn(
            Optional.of(
                SubwayRouteStation(
                    id = "K450",
                    routeID = 1004,
                    name = "중앙",
                    order = 50,
                    cumulativeTime = Duration.ofMinutes(5),
                    route = null,
                    stationName = null,
                    realtime = null,
                    timetable = null,
                ),
            ),
        )
        whenever(stationRepository.findById("K410")).thenReturn(
            Optional.of(
                SubwayRouteStation(
                    id = "K410",
                    routeID = 1004,
                    name = "시청",
                    order = 10,
                    cumulativeTime = Duration.ofMinutes(0),
                    route = null,
                    stationName = null,
                    realtime = null,
                    timetable = null,
                ),
            ),
        )
        whenever(stationRepository.findById("K456")).thenReturn(
            Optional.of(
                SubwayRouteStation(
                    id = "K456",
                    routeID = 1004,
                    name = "용산",
                    order = 60,
                    cumulativeTime = Duration.ofMinutes(10),
                    route = null,
                    stationName = null,
                    realtime = null,
                    timetable = null,
                ),
            ),
        )
        whenever(
            timetableRepository.findByStationIDAndHeadingAndWeekdayAndDepartureTime(
                stationID = "K450",
                heading = "up",
                weekday = "weekdays",
                departureTime = LocalTime.parse("11:00"),
            ),
        ).thenReturn(null)
        whenever(
            timetableRepository.save(
                SubwayTimetable(
                    stationID = "K450",
                    startStationID = "K410",
                    terminalStationID = "K456",
                    departureTime = LocalTime.parse("11:00"),
                    weekday = "weekdays",
                    heading = "up",
                    station = null,
                    startStation = null,
                    terminalStation = null,
                ),
            ),
        ).thenReturn(newTimetable)
        val createdTimetable =
            service.createTimetable(
                "K450",
                SubwayTimetableRequest(
                    startStationID = "K410",
                    terminalStationID = "K456",
                    departureTime = "11:00:00",
                    weekday =
                        "weekdays",
                    direction = "up",
                ),
            )
        assertEquals(3, createdTimetable.seq)
        assertEquals("K450", createdTimetable.stationID)
        assertEquals("K410", createdTimetable.startStationID)
        assertEquals("K456", createdTimetable.terminalStationID)
        assertEquals(LocalTime.parse("11:00"), createdTimetable.departureTime)
        assertEquals("weekdays", createdTimetable.weekday)
        assertEquals("up", createdTimetable.heading)
    }

    @Test
    @DisplayName("지하철 시간표 생성 - 잘못된 출발 시간 형식 예외 처리")
    fun testCreateTimetableInvalidTimeFormatException() {
        assertThrows<LocalTimeNotValidException> {
            service.createTimetable(
                "K450",
                SubwayTimetableRequest(
                    startStationID = "K410",
                    terminalStationID = "K456",
                    departureTime = "invalid_format",
                    weekday = "weekdays",
                    direction = "up",
                ),
            )
        }
    }

    @Test
    @DisplayName("지하철 시간표 생성 - 없는 역 ID 예외 처리")
    fun testCreateTimetableStationNotFoundException() {
        whenever(stationRepository.findById("K450")).thenReturn(Optional.empty())
        assertThrows<SubwayStationNotFoundException> {
            service.createTimetable(
                "K450",
                SubwayTimetableRequest(
                    startStationID = "K410",
                    terminalStationID = "K456",
                    departureTime = "11:00:00",
                    weekday = "weekdays",
                    direction = "up",
                ),
            )
        }
    }

    @Test
    @DisplayName("지하철 시간표 생성 - 없는 시점 역 ID 예외 처리")
    fun testCreateTimetableStartStationNotFoundException() {
        whenever(stationRepository.findById("K450")).thenReturn(
            Optional.of(
                SubwayRouteStation(
                    id = "K450",
                    routeID = 1004,
                    name = "중앙",
                    order = 50,
                    cumulativeTime = Duration.ofMinutes(5),
                    route = null,
                    stationName = null,
                    realtime = null,
                    timetable = null,
                ),
            ),
        )
        whenever(stationRepository.findById("K410")).thenReturn(Optional.empty())
        assertThrows<SubwayStartStationNotFoundException> {
            service.createTimetable(
                "K450",
                SubwayTimetableRequest(
                    startStationID = "K410",
                    terminalStationID = "K456",
                    departureTime = "11:00:00",
                    weekday = "weekdays",
                    direction = "up",
                ),
            )
        }
    }

    @Test
    @DisplayName("지하철 시간표 생성 - 없는 종점 역 ID 예외 처리")
    fun testCreateTimetableTerminalStationNotFoundException() {
        whenever(stationRepository.findById("K450")).thenReturn(
            Optional.of(
                SubwayRouteStation(
                    id = "K450",
                    routeID = 1004,
                    name = "중앙",
                    order = 50,
                    cumulativeTime = Duration.ofMinutes(5),
                    route = null,
                    stationName = null,
                    realtime = null,
                    timetable = null,
                ),
            ),
        )
        whenever(stationRepository.findById("K410")).thenReturn(
            Optional.of(
                SubwayRouteStation(
                    id = "K410",
                    routeID = 1004,
                    name = "시청",
                    order = 10,
                    cumulativeTime = Duration.ofMinutes(0),
                    route = null,
                    stationName = null,
                    realtime = null,
                    timetable = null,
                ),
            ),
        )
        whenever(stationRepository.findById("K456")).thenReturn(Optional.empty())
        assertThrows<SubwayTerminalStationNotFoundException> {
            service.createTimetable(
                "K450",
                SubwayTimetableRequest(
                    startStationID = "K410",
                    terminalStationID = "K456",
                    departureTime = "11:00:00",
                    weekday = "weekdays",
                    direction = "up",
                ),
            )
        }
    }

    @Test
    @DisplayName("지하철 시간표 생성 - 중복된 시간표 예외 처리")
    fun testCreateTimetableDuplicateTimetableException() {
        whenever(stationRepository.findById("K450")).thenReturn(
            Optional.of(
                SubwayRouteStation(
                    id = "K450",
                    routeID = 1004,
                    name = "중앙",
                    order = 50,
                    cumulativeTime = Duration.ofMinutes(5),
                    route = null,
                    stationName = null,
                    realtime = null,
                    timetable = null,
                ),
            ),
        )
        whenever(stationRepository.findById("K410")).thenReturn(
            Optional.of(
                SubwayRouteStation(
                    id = "K410",
                    routeID = 1004,
                    name = "시청",
                    order = 10,
                    cumulativeTime = Duration.ofMinutes(0),
                    route = null,
                    stationName = null,
                    realtime = null,
                    timetable = null,
                ),
            ),
        )
        whenever(stationRepository.findById("K456")).thenReturn(
            Optional.of(
                SubwayRouteStation(
                    id = "K456",
                    routeID = 1004,
                    name = "용산",
                    order = 60,
                    cumulativeTime = Duration.ofMinutes(10),
                    route = null,
                    stationName = null,
                    realtime = null,
                    timetable = null,
                ),
            ),
        )
        whenever(
            timetableRepository.findByStationIDAndHeadingAndWeekdayAndDepartureTime(
                stationID = "K450",
                heading = "up",
                weekday = "weekdays",
                departureTime = LocalTime.parse("09:00"),
            ),
        ).thenReturn(
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
        )
        assertThrows<DuplicateSubwayTimetableException> {
            service.createTimetable(
                "K450",
                SubwayTimetableRequest(
                    startStationID = "K410",
                    terminalStationID = "K456",
                    departureTime = "09:00:00",
                    weekday = "weekdays",
                    direction = "up",
                ),
            )
        }
    }

    @Test
    @DisplayName("지하철 시간표 수정")
    fun testUpdateTimetable() {
        val existingTimetable =
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
        val updatedTimetable =
            SubwayTimetable(
                seq = 1,
                stationID = "K450",
                startStationID = "K410",
                terminalStationID = "K456",
                departureTime = LocalTime.parse("10:00"),
                weekday = "weekdays",
                heading = "up",
                station = null,
                startStation = null,
                terminalStation = null,
            )
        whenever(timetableRepository.findById(1)).thenReturn(Optional.of(existingTimetable))
        whenever(stationRepository.findById("K450")).thenReturn(
            Optional.of(
                SubwayRouteStation(
                    id = "K450",
                    routeID = 1004,
                    name = "중앙",
                    order = 50,
                    cumulativeTime = Duration.ofMinutes(5),
                    route = null,
                    stationName = null,
                    realtime = null,
                    timetable = null,
                ),
            ),
        )
        whenever(stationRepository.findById("K410")).thenReturn(
            Optional.of(
                SubwayRouteStation(
                    id = "K410",
                    routeID = 1004,
                    name = "시청",
                    order = 10,
                    cumulativeTime = Duration.ofMinutes(0),
                    route = null,
                    stationName = null,
                    realtime = null,
                    timetable = null,
                ),
            ),
        )
        whenever(stationRepository.findById("K456")).thenReturn(
            Optional.of(
                SubwayRouteStation(
                    id = "K456",
                    routeID = 1004,
                    name = "용산",
                    order = 60,
                    cumulativeTime = Duration.ofMinutes(10),
                    route = null,
                    stationName = null,
                    realtime = null,
                    timetable = null,
                ),
            ),
        )
        whenever(timetableRepository.save(existingTimetable)).thenReturn(updatedTimetable)
        val result =
            service.updateTimetable(
                "K450",
                1,
                SubwayTimetableRequest(
                    startStationID = "K410",
                    terminalStationID = "K456",
                    departureTime = "10:00:00",
                    weekday = "weekdays",
                    direction = "up",
                ),
            )
        assertEquals(1, result.seq)
        assertEquals("K450", result.stationID)
        assertEquals("K410", result.startStationID)
        assertEquals("K456", result.terminalStationID)
        assertEquals(LocalTime.parse("10:00"), result.departureTime)
        assertEquals("weekdays", result.weekday)
        assertEquals("up", result.heading)
    }

    @Test
    @DisplayName("지하철 시간표 수정 - 없는 ID 예외 처리")
    fun testUpdateTimetableNotFoundException() {
        whenever(timetableRepository.findById(999)).thenReturn(Optional.empty())
        assertThrows<SubwayTimetableNotFoundException> {
            service.updateTimetable(
                "K450",
                999,
                SubwayTimetableRequest(
                    startStationID = "K410",
                    terminalStationID = "K456",
                    departureTime = "10:00:00",
                    weekday = "weekdays",
                    direction = "up",
                ),
            )
        }
    }

    @Test
    @DisplayName("지하철 시간표 수정 - 잘못된 출발 시간 형식 예외 처리")
    fun testUpdateTimetableInvalidTimeFormatException() {
        assertThrows<LocalTimeNotValidException> {
            service.updateTimetable(
                "K450",
                1,
                SubwayTimetableRequest(
                    startStationID = "K410",
                    terminalStationID = "K456",
                    departureTime = "invalid_format",
                    weekday = "weekdays",
                    direction = "up",
                ),
            )
        }
    }

    @Test
    @DisplayName("지하철 시간표 수정 - 없는 역 ID 예외 처리")
    fun testUpdateTimetableStationNotFoundException() {
        val existingTimetable =
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
        whenever(timetableRepository.findById(1)).thenReturn(Optional.of(existingTimetable))
        whenever(stationRepository.findById("K450")).thenReturn(Optional.empty())
        assertThrows<SubwayStationNotFoundException> {
            service.updateTimetable(
                "K450",
                1,
                SubwayTimetableRequest(
                    startStationID = "K410",
                    terminalStationID = "K456",
                    departureTime = "10:00:00",
                    weekday = "weekdays",
                    direction = "up",
                ),
            )
        }
    }

    @Test
    @DisplayName("지하철 시간표 수정 - 없는 시점 역 ID 예외 처리")
    fun testUpdateTimetableStartStationNotFoundException() {
        val existingTimetable =
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
        whenever(timetableRepository.findById(1)).thenReturn(Optional.of(existingTimetable))
        whenever(stationRepository.findById("K450")).thenReturn(
            Optional.of(
                SubwayRouteStation(
                    id = "K450",
                    routeID = 1004,
                    name = "중앙",
                    order = 50,
                    cumulativeTime = Duration.ofMinutes(5),
                    route = null,
                    stationName = null,
                    realtime = null,
                    timetable = null,
                ),
            ),
        )
        whenever(stationRepository.findById("K410")).thenReturn(Optional.empty())
        assertThrows<SubwayStartStationNotFoundException> {
            service.updateTimetable(
                "K450",
                1,
                SubwayTimetableRequest(
                    startStationID = "K410",
                    terminalStationID = "K456",
                    departureTime = "10:00:00",
                    weekday = "weekdays",
                    direction = "up",
                ),
            )
        }
    }

    @Test
    @DisplayName("지하철 시간표 수정 - 없는 종점 역 ID 예외 처리")
    fun testUpdateTimetableTerminalStationNotFoundException() {
        val existingTimetable =
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
        whenever(timetableRepository.findById(1)).thenReturn(Optional.of(existingTimetable))
        whenever(stationRepository.findById("K450")).thenReturn(
            Optional.of(
                SubwayRouteStation(
                    id = "K450",
                    routeID = 1004,
                    name = "중앙",
                    order = 50,
                    cumulativeTime = Duration.ofMinutes(5),
                    route = null,
                    stationName = null,
                    realtime = null,
                    timetable = null,
                ),
            ),
        )
        whenever(stationRepository.findById("K410")).thenReturn(
            Optional.of(
                SubwayRouteStation(
                    id = "K410",
                    routeID = 1004,
                    name = "시청",
                    order = 10,
                    cumulativeTime = Duration.ofMinutes(0),
                    route = null,
                    stationName = null,
                    realtime = null,
                    timetable = null,
                ),
            ),
        )
        whenever(stationRepository.findById("K456")).thenReturn(Optional.empty())
        assertThrows<SubwayTerminalStationNotFoundException> {
            service.updateTimetable(
                "K450",
                1,
                SubwayTimetableRequest(
                    startStationID = "K410",
                    terminalStationID = "K456",
                    departureTime = "10:00:00",
                    weekday = "weekdays",
                    direction = "up",
                ),
            )
        }
    }

    @Test
    @DisplayName("지하철 시간표 삭제")
    fun testDeleteTimetable() {
        val existingTimetable =
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
        whenever(stationRepository.findById("K450")).thenReturn(
            Optional.of(
                SubwayRouteStation(
                    id = "K450",
                    routeID = 1004,
                    name = "중앙",
                    order = 50,
                    cumulativeTime = Duration.ofMinutes(5),
                    route = null,
                    stationName = null,
                    realtime = null,
                    timetable = null,
                ),
            ),
        )
        whenever(timetableRepository.findById(1)).thenReturn(Optional.of(existingTimetable))
        service.deleteTimetable("K450", 1)
        verify(timetableRepository).delete(existingTimetable)
    }

    @Test
    @DisplayName("지하철 시간표 삭제 - 없는 ID 예외 처리")
    fun testDeleteTimetableNotFoundException() {
        whenever(stationRepository.findById("K450")).thenReturn(
            Optional.of(
                SubwayRouteStation(
                    id = "K450",
                    routeID = 1004,
                    name = "중앙",
                    order = 50,
                    cumulativeTime = Duration.ofMinutes(5),
                    route = null,
                    stationName = null,
                    realtime = null,
                    timetable = null,
                ),
            ),
        )
        whenever(timetableRepository.findById(999)).thenReturn(Optional.empty())
        assertThrows<SubwayTimetableNotFoundException> {
            service.deleteTimetable("K450", 999)
        }
    }

    @Test
    @DisplayName("지하철 시간표 삭제 - 존재하지 않는 역 ID 예외 처리")
    fun testDeleteTimetableStationNotFoundException() {
        assertThrows<SubwayStationNotFoundException> {
            service.deleteTimetable("K251", 1)
        }
    }

    @Test
    @DisplayName("지하철 시간표 삭제 - 역 ID 불일치 예외 처리")
    fun testDeleteTimetableStationIDMismatchException() {
        val existingTimetable =
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
        whenever(stationRepository.findById("K251")).thenReturn(
            Optional.of(
                SubwayRouteStation(
                    id = "K251",
                    routeID = 1001,
                    name = "서울역",
                    order = 1,
                    cumulativeTime = Duration.ofMinutes(5),
                    route = null,
                    stationName = null,
                    realtime = null,
                    timetable = null,
                ),
            ),
        )
        whenever(timetableRepository.findById(1)).thenReturn(Optional.of(existingTimetable))
        assertThrows<SubwayTimetableNotFoundException> {
            service.deleteTimetable("K251", 1)
        }
    }

    @Test
    @DisplayName("지하철 실시간 도착 정보 목록 조회")
    fun testGetAllRealtime() {
        whenever(realtimeRepository.findAll()).thenReturn(
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
        val realtimeList = service.getRealtimeList()
        assertEquals(2, realtimeList.size)
        assertEquals("K450", realtimeList[0].stationID)
        assertEquals("K251", realtimeList[1].stationID)
    }
}
