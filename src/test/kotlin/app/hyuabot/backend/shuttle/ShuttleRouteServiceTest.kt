package app.hyuabot.backend.shuttle

import app.hyuabot.backend.database.entity.ShuttleRoute
import app.hyuabot.backend.database.entity.ShuttleRouteStop
import app.hyuabot.backend.database.entity.ShuttleStop
import app.hyuabot.backend.database.entity.ShuttleTimetable
import app.hyuabot.backend.database.exception.DurationNotValidException
import app.hyuabot.backend.database.exception.LocalTimeNotValidException
import app.hyuabot.backend.database.repository.ShuttleRouteRepository
import app.hyuabot.backend.database.repository.ShuttleRouteStopRepository
import app.hyuabot.backend.database.repository.ShuttleStopRepository
import app.hyuabot.backend.database.repository.ShuttleTimetableRepository
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
import java.util.Optional
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class ShuttleRouteServiceTest {
    @Mock private lateinit var shuttleRouteRepository: ShuttleRouteRepository

    @Mock private lateinit var shuttleStopRepository: ShuttleStopRepository

    @Mock private lateinit var shuttleRouteStopRepository: ShuttleRouteStopRepository

    @Mock private lateinit var shuttleTimetableRepository: ShuttleTimetableRepository

    @InjectMocks private lateinit var shuttleRouteService: ShuttleRouteService

    @Test
    @DisplayName("셔틀버스 노선 목록 조회")
    fun getAllRoutes() {
        whenever(shuttleRouteRepository.findAll()).thenReturn(
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
        )
        assertEquals(2, shuttleRouteService.getAllRoutes().size)
        assertEquals(2, shuttleRouteService.getAllRoutes("").size)
    }

    @Test
    @DisplayName("셔틀버스 노선 목록 조회 (이름 필터링)")
    fun getRoutesByName() {
        val nameFilter = "A"
        whenever(shuttleRouteRepository.findByNameContaining(nameFilter)).thenReturn(
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
            ),
        )
        assertEquals(1, shuttleRouteService.getAllRoutes(nameFilter).size)
    }

    @Test
    @DisplayName("셔틀버스 노선 이름으로 조회")
    fun getRouteByName() {
        val routeName = "A"
        val expectedRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "A 노선",
                descriptionEnglish = "Route A",
                tag = "A",
                startStopID = "Stop 1",
                endStopID = "Stop 2",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(expectedRoute))
        assertEquals(expectedRoute, shuttleRouteService.getRouteByName(routeName))
    }

    @Test
    @DisplayName("셔틀버스 노선 이름으로 조회 (존재하지 않는 경우)")
    fun getRouteByNameNotFound() {
        val routeName = "NonExistentRoute"
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.empty())
        assertThrows<ShuttleRouteNotFoundException> {
            shuttleRouteService.getRouteByName(routeName)
        }
    }

    @Test
    @DisplayName("셔틀버스 노선 생성")
    fun createRoute() {
        val newRoute =
            ShuttleRoute(
                name = "NewRoute",
                descriptionKorean = "새로운 노선",
                descriptionEnglish = "New Route",
                tag = "NR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        val payload =
            CreateShuttleRouteRequest(
                name = newRoute.name,
                descriptionKorean = newRoute.descriptionKorean,
                descriptionEnglish = newRoute.descriptionEnglish,
                tag = newRoute.tag,
                startStopID = newRoute.startStopID,
                endStopID = newRoute.endStopID,
            )
        whenever(shuttleRouteRepository.save(newRoute)).thenReturn(newRoute)
        assertEquals(newRoute, shuttleRouteService.createRoute(payload))
    }

    @Test
    @DisplayName("셔틀버스 노선 생성 실패 (중복된 이름)")
    fun createRouteDuplicateName() {
        val existingRoute =
            ShuttleRoute(
                name = "ExistingRoute",
                descriptionKorean = "기존 노선",
                descriptionEnglish = "Existing Route",
                tag = "ER",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        val payload =
            CreateShuttleRouteRequest(
                name = existingRoute.name,
                descriptionKorean = existingRoute.descriptionKorean,
                descriptionEnglish = existingRoute.descriptionEnglish,
                tag = existingRoute.tag,
                startStopID = existingRoute.startStopID,
                endStopID = existingRoute.endStopID,
            )
        whenever(shuttleRouteRepository.existsById(existingRoute.name)).thenReturn(true)
        assertThrows<DuplicateShuttleRouteException> {
            shuttleRouteService.createRoute(payload)
        }
    }

    @Test
    @DisplayName("셔틀버스 노선 수정")
    fun updateRoute() {
        val existingRoute =
            ShuttleRoute(
                name = "ExistingRoute",
                descriptionKorean = "기존 노선",
                descriptionEnglish = "Existing Route",
                tag = "ER",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        val updatedDescriptionKorean = "수정된 노선"
        val updatedDescriptionEnglish = "Updated Route"
        existingRoute.descriptionKorean = updatedDescriptionKorean
        existingRoute.descriptionEnglish = updatedDescriptionEnglish

        whenever(shuttleRouteRepository.findById(existingRoute.name)).thenReturn(Optional.of(existingRoute))
        whenever(shuttleRouteRepository.save(existingRoute)).thenReturn(existingRoute)

        assertEquals(
            existingRoute,
            shuttleRouteService.updateRoute(
                existingRoute.name,
                UpdateShuttleRouteRequest(
                    descriptionKorean = updatedDescriptionKorean,
                    descriptionEnglish = updatedDescriptionEnglish,
                    tag = existingRoute.tag,
                    startStopID = existingRoute.startStopID,
                    endStopID = existingRoute.endStopID,
                ),
            ),
        )
    }

    @Test
    @DisplayName("셔틀버스 노선 수정 실패 (존재하지 않는 노선 이름)")
    fun updateRouteNotFound() {
        val routeName = "NonExistentRoute"
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.empty())
        assertThrows<ShuttleRouteNotFoundException> {
            shuttleRouteService.updateRoute(
                routeName,
                UpdateShuttleRouteRequest(
                    descriptionKorean = "수정된 노선",
                    descriptionEnglish = "Updated Route",
                    tag = "UR",
                    startStopID = "StartStop",
                    endStopID = "EndStop",
                ),
            )
        }
    }

    @Test
    @DisplayName("셔틀버스 노선 삭제")
    fun deleteRoute() {
        val routeName = "RouteToDelete"
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "삭제할 노선",
                descriptionEnglish = "Route to Delete",
                tag = "RTD",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        shuttleRouteService.deleteRouteByName(routeName)
        verify(shuttleRouteRepository).deleteById(routeName)
    }

    @Test
    @DisplayName("셔틀버스 노선 삭제 실패 (존재하지 않는 노선 이름)")
    fun deleteRouteNotFound() {
        val routeName = "NonExistentRoute"
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.empty())
        assertThrows<ShuttleRouteNotFoundException> {
            shuttleRouteService.deleteRouteByName(routeName)
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 목록 조회")
    fun getStopsByRouteName() {
        val routeName = "TestRoute"
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        val stops =
            listOf(
                ShuttleRouteStop(
                    stopName = "Stop1",
                    routeName = routeName,
                    order = 1,
                    cumulativeTime = Duration.ofMinutes(5),
                    route = null,
                    stop = null,
                ),
                ShuttleRouteStop(
                    stopName = "Stop2",
                    routeName = routeName,
                    order = 2,
                    cumulativeTime = Duration.ofMinutes(10),
                    route = null,
                    stop = null,
                ),
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        whenever(shuttleRouteStopRepository.findByRouteName(routeName)).thenReturn(stops)
        val result = shuttleRouteService.getShuttleStopByRouteName(routeName)
        assertEquals(2, result.size)
        assertEquals("Stop1", result[0].stopName)
        assertEquals("Stop2", result[1].stopName)
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 목록 조회 실패 (존재하지 않는 노선 이름)")
    fun getStopsByRouteNameNotFound() {
        val routeName = "NonExistentRoute"
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.empty())
        assertThrows<ShuttleRouteNotFoundException> {
            shuttleRouteService.getShuttleStopByRouteName(routeName)
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 추가")
    fun addStopToRoute() {
        val routeName = "TestRoute"
        val stopName = "NewStop"
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        val existingStop =
            ShuttleStop(
                name = stopName,
                latitude = 37.5665,
                longitude = 126.978,
                route = emptyList(),
                routeToStart = emptyList(),
                routeToEnd = emptyList(),
            )
        val newRouteStop =
            ShuttleRouteStop(
                stopName = stopName,
                routeName = routeName,
                order = 3,
                cumulativeTime = Duration.ofMinutes(15),
                route = null,
                stop = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        whenever(shuttleStopRepository.findById(stopName)).thenReturn(Optional.of(existingStop))
        whenever(shuttleRouteStopRepository.save(newRouteStop)).thenReturn(newRouteStop)

        val result =
            shuttleRouteService.createShuttleRouteStop(
                routeName,
                CreateShuttleRouteStopRequest(
                    stopName = newRouteStop.stopName,
                    order = newRouteStop.order,
                    cumulativeTime = "00:15:00",
                ),
            )
        assertEquals(newRouteStop.stopName, result.stopName)
        assertEquals(newRouteStop.routeName, result.routeName)
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 추가 실패 (존재하지 않는 노선 이름)")
    fun addStopToRouteNotFound() {
        val routeName = "NonExistentRoute"
        val stopName = "NewStop"
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.empty())
        assertThrows<ShuttleRouteNotFoundException> {
            shuttleRouteService.createShuttleRouteStop(
                routeName,
                CreateShuttleRouteStopRequest(
                    stopName = stopName,
                    order = 1,
                    cumulativeTime = "00:05:00",
                ),
            )
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 추가 실패 (존재하지 않는 정류장 이름)")
    fun addStopToRouteStopNotFound() {
        val routeName = "TestRoute"
        val stopName = "NonExistentStop"
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        whenever(shuttleStopRepository.findById(stopName)).thenReturn(Optional.empty())
        assertThrows<ShuttleStopNotFoundException> {
            shuttleRouteService.createShuttleRouteStop(
                routeName,
                CreateShuttleRouteStopRequest(
                    stopName = stopName,
                    order = 1,
                    cumulativeTime = "00:05:00",
                ),
            )
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 추가 실패 (중복된 정류장 이름)")
    fun addStopToRouteDuplicateStop() {
        val routeName = "TestRoute"
        val stopName = "ExistingStop"
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        val existingStop =
            ShuttleStop(
                name = stopName,
                latitude = 37.5665,
                longitude = 126.978,
                route = emptyList(),
                routeToStart = emptyList(),
                routeToEnd = emptyList(),
            )
        val existingRouteStop =
            ShuttleRouteStop(
                stopName = stopName,
                routeName = routeName,
                order = 1,
                cumulativeTime = Duration.ofMinutes(5),
                route = null,
                stop = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        whenever(shuttleStopRepository.findById(stopName)).thenReturn(Optional.of(existingStop))
        whenever(shuttleRouteStopRepository.findByRouteNameAndStopName(routeName, stopName)).thenReturn(existingRouteStop)

        assertThrows<DuplicateShuttleRouteStopException> {
            shuttleRouteService.createShuttleRouteStop(
                routeName,
                CreateShuttleRouteStopRequest(
                    stopName = stopName,
                    order = 1,
                    cumulativeTime = "00:05:00",
                ),
            )
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 추가 실패 (잘못된 소요 시간 형식)")
    fun addStopToRouteInvalidCumulativeTime() {
        val routeName = "TestRoute"
        val stopName = "NewStop"
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        val existingStop =
            ShuttleStop(
                name = stopName,
                latitude = 37.5665,
                longitude = 126.978,
                route = emptyList(),
                routeToStart = emptyList(),
                routeToEnd = emptyList(),
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        whenever(shuttleStopRepository.findById(stopName)).thenReturn(Optional.of(existingStop))

        assertThrows<DurationNotValidException> {
            shuttleRouteService.createShuttleRouteStop(
                routeName,
                CreateShuttleRouteStopRequest(
                    stopName = stopName,
                    order = 1,
                    cumulativeTime = "invalid_time_format",
                ),
            )
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 상세 조회")
    fun getStopInRoute() {
        val routeName = "TestRoute"
        val stopName = "ExistingStop"
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        val existingRouteStop =
            ShuttleRouteStop(
                stopName = stopName,
                routeName = routeName,
                order = 1,
                cumulativeTime = Duration.ofMinutes(5),
                route = null,
                stop = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        whenever(
            shuttleRouteStopRepository.findByRouteNameAndStopName(
                routeName,
                stopName,
            ),
        ).thenReturn(existingRouteStop)

        val result =
            shuttleRouteService.getShuttleRouteStopByRouteNameAndStopName(routeName, stopName)
        assertEquals(existingRouteStop.stopName, result.stopName)
        assertEquals(existingRouteStop.routeName, result.routeName)
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 상세 조회 실패 (존재하지 않는 노선 이름)")
    fun getStopInRouteNotFound() {
        val routeName = "NonExistentRoute"
        val stopName = "ExistingStop"
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.empty())
        assertThrows<ShuttleRouteNotFoundException> {
            shuttleRouteService.getShuttleRouteStopByRouteNameAndStopName(routeName, stopName)
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 상세 조회 실패 (존재하지 않는 정류장 이름)")
    fun getStopInRouteStopNotFound() {
        val routeName = "TestRoute"
        val stopName = "NonExistentStop"
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        whenever(
            shuttleRouteStopRepository.findByRouteNameAndStopName(
                routeName,
                stopName,
            ),
        ).thenReturn(null)

        assertThrows<ShuttleRouteStopNotFoundException> {
            shuttleRouteService.getShuttleRouteStopByRouteNameAndStopName(routeName, stopName)
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 수정")
    fun updateStopInRoute() {
        val existingRoute =
            ShuttleRoute(
                name = "TestRoute",
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        val existingRouteStop =
            ShuttleRouteStop(
                stopName = "ExistingStop",
                routeName = "TestRoute",
                order = 1,
                cumulativeTime = Duration.ofMinutes(5),
                route = null,
                stop = null,
            )
        whenever(shuttleRouteRepository.findById(existingRoute.name)).thenReturn(Optional.of(existingRoute))
        whenever(
            shuttleRouteStopRepository.findByRouteNameAndStopName(
                existingRoute.name,
                existingRouteStop.stopName,
            ),
        ).thenReturn(existingRouteStop)
        whenever(shuttleRouteStopRepository.save(existingRouteStop)).thenReturn(
            existingRouteStop.apply {
                order = 2
                cumulativeTime = Duration.ofMinutes(10)
            },
        )
        shuttleRouteService.updateShuttleRouteStop(
            existingRoute.name,
            existingRouteStop.stopName,
            UpdateShuttleRouteStopRequest(
                order = 2,
                cumulativeTime = "00:10:00",
            ),
        )
        verify(shuttleRouteStopRepository).save(existingRouteStop)
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 수정 실패 (존재하지 않는 노선 이름)")
    fun updateStopInRouteNotFound() {
        val routeName = "NonExistentRoute"
        val stopName = "ExistingStop"
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.empty())
        assertThrows<ShuttleRouteNotFoundException> {
            shuttleRouteService.updateShuttleRouteStop(
                routeName,
                stopName,
                UpdateShuttleRouteStopRequest(
                    order = 1,
                    cumulativeTime = "00:05:00",
                ),
            )
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 수정 실패 (존재하지 않는 정류장 이름)")
    fun updateStopInRouteStopNotFound() {
        val routeName = "TestRoute"
        val stopName = "NonExistentStop"
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        assertThrows<ShuttleRouteStopNotFoundException> {
            shuttleRouteService.updateShuttleRouteStop(
                routeName,
                stopName,
                UpdateShuttleRouteStopRequest(
                    order = 1,
                    cumulativeTime = "00:05:00",
                ),
            )
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 수정 실패 (잘못된 소요 시간 형식)")
    fun updateStopInRouteInvalidCumulativeTime() {
        val routeName = "TestRoute"
        val stopName = "ExistingStop"
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        val existingRouteStop =
            ShuttleRouteStop(
                stopName = stopName,
                routeName = routeName,
                order = 1,
                cumulativeTime = Duration.ofMinutes(5),
                route = null,
                stop = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        whenever(
            shuttleRouteStopRepository.findByRouteNameAndStopName(
                routeName,
                stopName,
            ),
        ).thenReturn(existingRouteStop)

        assertThrows<DurationNotValidException> {
            shuttleRouteService.updateShuttleRouteStop(
                routeName,
                stopName,
                UpdateShuttleRouteStopRequest(
                    order = 2,
                    cumulativeTime = "invalid_time_format",
                ),
            )
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 삭제")
    fun deleteStopFromRoute() {
        val routeName = "TestRoute"
        val stopName = "StopToDelete"
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        val existingRouteStop =
            ShuttleRouteStop(
                stopName = stopName,
                routeName = routeName,
                order = 1,
                cumulativeTime = Duration.ofMinutes(5),
                route = null,
                stop = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        whenever(
            shuttleRouteStopRepository.findByRouteNameAndStopName(
                routeName,
                stopName,
            ),
        ).thenReturn(existingRouteStop)

        shuttleRouteService.deleteShuttleRouteStop(routeName, stopName)
        verify(shuttleRouteStopRepository).delete(existingRouteStop)
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 삭제 실패 (존재하지 않는 노선 이름)")
    fun deleteStopFromRouteNotFound() {
        val routeName = "NonExistentRoute"
        val stopName = "StopToDelete"
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.empty())
        assertThrows<ShuttleRouteNotFoundException> {
            shuttleRouteService.deleteShuttleRouteStop(routeName, stopName)
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 삭제 실패 (존재하지 않는 정류장 이름)")
    fun deleteStopFromRouteStopNotFound() {
        val routeName = "TestRoute"
        val stopName = "NonExistentStop"
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        assertThrows<ShuttleRouteStopNotFoundException> {
            shuttleRouteService.deleteShuttleRouteStop(routeName, stopName)
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 목록 조회")
    fun getTimetableByRouteName() {
        val routeName = "TestRoute"
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        whenever(shuttleTimetableRepository.findByRouteName(routeName)).thenReturn(
            listOf(
                ShuttleTimetable(
                    seq = 1,
                    periodType = "semester",
                    weekday = true,
                    routeName = routeName,
                    departureTime = LocalTime.parse("08:00:00"),
                    route = null,
                ),
                ShuttleTimetable(
                    seq = 2,
                    periodType = "semester",
                    weekday = true,
                    routeName = routeName,
                    departureTime = LocalTime.parse("09:00:00"),
                    route = null,
                ),
            ),
        )
        val result = shuttleRouteService.getShuttleTimetableByRouteName(routeName, null, null)
        assertEquals(2, result.size)
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 목록 조회 (기간 필터링)")
    fun getTimetableByRouteNameWithPeriod() {
        val routeName = "TestRoute"
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        whenever(shuttleTimetableRepository.findByPeriodTypeAndRouteName("semester", routeName)).thenReturn(
            listOf(
                ShuttleTimetable(
                    seq = 1,
                    periodType = "semester",
                    weekday = true,
                    routeName = routeName,
                    departureTime = LocalTime.parse("08:00:00"),
                    route = null,
                ),
            ),
        )
        val result = shuttleRouteService.getShuttleTimetableByRouteName(routeName, "semester", null)
        assertEquals(1, result.size)
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 목록 조회 (요일 필터링)")
    fun getTimetableByRouteNameWithWeekday() {
        val routeName = "TestRoute"
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        whenever(shuttleTimetableRepository.findByRouteNameAndWeekday(routeName, true)).thenReturn(
            listOf(
                ShuttleTimetable(
                    seq = 1,
                    periodType = "semester",
                    weekday = true,
                    routeName = routeName,
                    departureTime = LocalTime.parse("08:00:00"),
                    route = null,
                ),
            ),
        )
        val result = shuttleRouteService.getShuttleTimetableByRouteName(routeName, null, true)
        assertEquals(1, result.size)
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 목록 조회 (요일 및 기간 필터링)")
    fun getTimetableByRouteNameWithWeekdayAndPeriod() {
        val routeName = "TestRoute"
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        whenever(shuttleTimetableRepository.findByPeriodTypeAndRouteNameAndWeekday("semester", routeName, true)).thenReturn(
            listOf(
                ShuttleTimetable(
                    seq = 1,
                    periodType = "semester",
                    weekday = true,
                    routeName = routeName,
                    departureTime = LocalTime.parse("08:00:00"),
                    route = null,
                ),
            ),
        )
        val result = shuttleRouteService.getShuttleTimetableByRouteName(routeName, "semester", true)
        assertEquals(1, result.size)
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 목록 조회 실패 (존재하지 않는 노선 이름)")
    fun getTimetableByRouteNameNotFound() {
        val routeName = "NonExistentRoute"
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.empty())
        assertThrows<ShuttleRouteNotFoundException> {
            shuttleRouteService.getShuttleTimetableByRouteName(routeName, null, null)
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 추가")
    fun addTimetableToRoute() {
        val routeName = "TestRoute"
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        val newTimetable =
            ShuttleTimetable(
                seq = null,
                periodType = "semester",
                weekday = true,
                routeName = routeName,
                departureTime = LocalTime.parse("08:00:00"),
                route = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        whenever(shuttleTimetableRepository.save(newTimetable)).thenReturn(newTimetable)

        val result =
            shuttleRouteService.createShuttleTimetable(
                routeName,
                ShuttleTimetableRequest(
                    periodType = newTimetable.periodType,
                    weekday = newTimetable.weekday,
                    departureTime = "08:00:00",
                ),
            )
        assertEquals(newTimetable.departureTime, result.departureTime)
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 추가 실패 (존재하지 않는 노선 이름)")
    fun addTimetableToRouteNotFound() {
        val routeName = "NonExistentRoute"
        val newTimetable =
            ShuttleTimetableRequest(
                periodType = "semester",
                weekday = true,
                departureTime = "08:00:00",
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.empty())
        assertThrows<ShuttleRouteNotFoundException> {
            shuttleRouteService.createShuttleTimetable(routeName, newTimetable)
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 추가 실패 (잘못된 출발 시간 형식)")
    fun addTimetableToRouteInvalidDepartureTime() {
        val routeName = "TestRoute"
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))

        assertThrows<LocalTimeNotValidException> {
            shuttleRouteService.createShuttleTimetable(
                routeName,
                ShuttleTimetableRequest(
                    periodType = "semester",
                    weekday = true,
                    departureTime = "invalid_time_format",
                ),
            )
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 추가 실패 (중복된 시간표)")
    fun addTimetableToRouteDuplicateTimetable() {
        val routeName = "TestRoute"
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        val existingTimetable =
            ShuttleTimetable(
                seq = 1,
                periodType = "semester",
                weekday = true,
                routeName = routeName,
                departureTime = LocalTime.parse("08:00:00"),
                route = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        whenever(
            shuttleTimetableRepository.findByRouteNameAndPeriodTypeAndWeekdayAndDepartureTime(
                routeName,
                existingTimetable.periodType,
                existingTimetable.weekday,
                existingTimetable.departureTime,
            ),
        ).thenReturn(existingTimetable)

        assertThrows<DuplicateShuttleTimetableException> {
            shuttleRouteService.createShuttleTimetable(
                routeName,
                ShuttleTimetableRequest(
                    periodType = existingTimetable.periodType,
                    weekday = existingTimetable.weekday,
                    departureTime = "08:00:00",
                ),
            )
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 조회")
    fun getTimetableBySeq() {
        val routeName = "TestRoute"
        val seq = 1
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        val existingTimetable =
            ShuttleTimetable(
                seq = seq,
                periodType = "semester",
                weekday = true,
                routeName = routeName,
                departureTime = LocalTime.parse("08:00:00"),
                route = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        whenever(shuttleTimetableRepository.findByRouteNameAndSeq(routeName, seq)).thenReturn(existingTimetable)

        val result = shuttleRouteService.getShuttleTimetableByRouteNameAndSeq(routeName, seq)
        assertEquals(existingTimetable, result)
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 조회 실패 (존재하지 않는 노선 이름)")
    fun getTimetableBySeqNotFound() {
        val routeName = "NonExistentRoute"
        val seq = 1
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.empty())
        assertThrows<ShuttleRouteNotFoundException> {
            shuttleRouteService.getShuttleTimetableByRouteNameAndSeq(routeName, seq)
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 조회 실패 (존재하지 않는 시간표 시퀀스)")
    fun getTimetableBySeqNotFoundTimetable() {
        val routeName = "TestRoute"
        val seq = 999
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        whenever(shuttleTimetableRepository.findByRouteNameAndSeq(routeName, seq)).thenReturn(null)

        assertThrows<ShuttleTimetableNotFoundException> {
            shuttleRouteService.getShuttleTimetableByRouteNameAndSeq(routeName, seq)
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 수정")
    fun updateTimetable() {
        val routeName = "TestRoute"
        val seq = 1
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        val existingTimetable =
            ShuttleTimetable(
                seq = seq,
                periodType = "semester",
                weekday = true,
                routeName = routeName,
                departureTime = LocalTime.parse("08:00:00"),
                route = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        whenever(shuttleTimetableRepository.findByRouteNameAndSeq(routeName, seq)).thenReturn(existingTimetable)
        whenever(shuttleTimetableRepository.save(existingTimetable)).thenReturn(
            existingTimetable.apply {
                departureTime = LocalTime.parse("09:00:00")
            },
        )

        shuttleRouteService.updateShuttleTimetable(
            routeName,
            seq,
            ShuttleTimetableRequest(
                periodType = existingTimetable.periodType,
                weekday = existingTimetable.weekday,
                departureTime = "09:00:00",
            ),
        )
        verify(shuttleTimetableRepository).save(existingTimetable)
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 수정 실패 (존재하지 않는 노선 이름)")
    fun updateTimetableNotFound() {
        val routeName = "NonExistentRoute"
        val seq = 1
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.empty())
        assertThrows<ShuttleRouteNotFoundException> {
            shuttleRouteService.updateShuttleTimetable(
                routeName,
                seq,
                ShuttleTimetableRequest(
                    periodType = "semester",
                    weekday = true,
                    departureTime = "09:00:00",
                ),
            )
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 수정 실패 (존재하지 않는 시간표 시퀀스)")
    fun updateTimetableNotFoundTimetable() {
        val routeName = "TestRoute"
        val seq = 999
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        whenever(shuttleTimetableRepository.findByRouteNameAndSeq(routeName, seq)).thenReturn(null)

        assertThrows<ShuttleTimetableNotFoundException> {
            shuttleRouteService.updateShuttleTimetable(
                routeName,
                seq,
                ShuttleTimetableRequest(
                    periodType = "semester",
                    weekday = true,
                    departureTime = "09:00:00",
                ),
            )
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 수정 실패 (잘못된 출발 시간 형식)")
    fun updateTimetableInvalidDepartureTime() {
        val routeName = "TestRoute"
        val seq = 1
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        val existingTimetable =
            ShuttleTimetable(
                seq = seq,
                periodType = "semester",
                weekday = true,
                routeName = routeName,
                departureTime = LocalTime.parse("08:00:00"),
                route = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        whenever(shuttleTimetableRepository.findByRouteNameAndSeq(routeName, seq)).thenReturn(existingTimetable)

        assertThrows<LocalTimeNotValidException> {
            shuttleRouteService.updateShuttleTimetable(
                routeName,
                seq,
                ShuttleTimetableRequest(
                    periodType = existingTimetable.periodType,
                    weekday = existingTimetable.weekday,
                    departureTime = "invalid_time_format",
                ),
            )
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 삭제")
    fun deleteTimetable() {
        val routeName = "TestRoute"
        val seq = 1
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        val existingTimetable =
            ShuttleTimetable(
                seq = seq,
                periodType = "semester",
                weekday = true,
                routeName = routeName,
                departureTime = LocalTime.parse("08:00:00"),
                route = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        whenever(shuttleTimetableRepository.findByRouteNameAndSeq(routeName, seq)).thenReturn(existingTimetable)

        shuttleRouteService.deleteShuttleTimetable(routeName, seq)
        verify(shuttleTimetableRepository).delete(existingTimetable)
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 삭제 실패 (존재하지 않는 노선 이름)")
    fun deleteTimetableNotFound() {
        val routeName = "NonExistentRoute"
        val seq = 1
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.empty())
        assertThrows<ShuttleRouteNotFoundException> {
            shuttleRouteService.deleteShuttleTimetable(routeName, seq)
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 시간표 삭제 실패 (존재하지 않는 시간표 시퀀스)")
    fun deleteTimetableNotFoundTimetable() {
        val routeName = "TestRoute"
        val seq = 999
        val existingRoute =
            ShuttleRoute(
                name = routeName,
                descriptionKorean = "테스트 노선",
                descriptionEnglish = "Test Route",
                tag = "TR",
                startStopID = "StartStop",
                endStopID = "EndStop",
                timetable = emptyList(),
                stop = emptyList(),
                startStop = null,
                endStop = null,
            )
        whenever(shuttleRouteRepository.findById(routeName)).thenReturn(Optional.of(existingRoute))
        whenever(shuttleTimetableRepository.findByRouteNameAndSeq(routeName, seq)).thenReturn(null)

        assertThrows<ShuttleTimetableNotFoundException> {
            shuttleRouteService.deleteShuttleTimetable(routeName, seq)
        }
    }
}
