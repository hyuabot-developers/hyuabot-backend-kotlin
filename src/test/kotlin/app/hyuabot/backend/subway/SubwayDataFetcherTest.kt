package app.hyuabot.backend.subway

import app.hyuabot.backend.codegen.types.SubwayArrival
import app.hyuabot.backend.codegen.types.SubwayArrivalGroup
import app.hyuabot.backend.codegen.types.SubwayOriginTerminal
import app.hyuabot.backend.database.entity.SubwayRealtime
import app.hyuabot.backend.database.entity.SubwayRoute
import app.hyuabot.backend.database.entity.SubwayRouteStation
import app.hyuabot.backend.database.entity.SubwayTimetable
import app.hyuabot.backend.subway.controller.SubwayDataFetcher
import app.hyuabot.backend.subway.controller.SubwayTimetableDataLoader
import app.hyuabot.backend.subway.domain.SubwayTimetableKey
import app.hyuabot.backend.subway.service.SubwayService
import app.hyuabot.backend.utility.ScalarRegistration
import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.test.EnableDgsTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertNotNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

@EnableDgsTest
@SpringJUnitConfig
@Import(SubwayDataFetcher::class, SubwayTimetableDataLoader::class, ScalarRegistration::class)
class SubwayDataFetcherTest {
    @Autowired private lateinit var dgsQueryExecutor: DgsQueryExecutor

    @MockitoBean private lateinit var subwayService: SubwayService

    private fun createRoute(
        id: Int = 1004,
        name: String = "4호선",
    ) = SubwayRoute(
        id = id,
        name = name,
        station = emptyList(),
    )

    private fun createStation(
        id: String = "K449",
        routeID: Int = 1004,
        name: String = "한대앞",
        order: Int = 49,
        cumulativeTime: Duration = Duration.ofMinutes(18),
        route: SubwayRoute? = null,
    ) = SubwayRouteStation(
        id = id,
        routeID = routeID,
        name = name,
        order = order,
        cumulativeTime = cumulativeTime,
        route = route,
        stationName = null,
        realtime = emptyList(),
        timetable = emptyList(),
    )

    private fun createTimetable(
        seq: Int = 1,
        stationID: String = "K449",
        startStationID: String = "K409",
        terminalStationID: String = "K456",
        departureTime: LocalTime = LocalTime.parse("09:00:00"),
        weekday: String = "weekdays",
        heading: String = "up",
        startStation: SubwayRouteStation? = null,
        terminalStation: SubwayRouteStation? = null,
    ) = SubwayTimetable(
        seq = seq,
        stationID = stationID,
        startStationID = startStationID,
        terminalStationID = terminalStationID,
        departureTime = departureTime,
        weekday = weekday,
        heading = heading,
        station = null,
        startStation = startStation,
        terminalStation = terminalStation,
    )

    private fun createRealtime(
        stationID: String = "K449",
        heading: String = "up",
        order: Int = 1,
        location: String = "중앙",
        remainingStop: Int = 1,
        remainingTime: Duration = Duration.ofMinutes(2),
        terminalStationID: String = "K409",
        trainNumber: String = "K4001",
        isExpress: Boolean = false,
        isLast: Boolean = false,
        status: Int = 99,
        terminalStation: SubwayRouteStation? = null,
    ) = SubwayRealtime(
        stationID = stationID,
        heading = heading,
        order = order,
        location = location,
        remainingStop = remainingStop,
        remainingTime = remainingTime,
        terminalStationID = terminalStationID,
        trainNumber = trainNumber,
        isExpress = isExpress,
        isLast = isLast,
        status = status,
        terminalStation = terminalStation,
        station = null,
        updatedAt = ZonedDateTime.now(),
    )

    private val route = createRoute()
    private val terminalStation =
        createStation(
            id = "K409",
            name = "당고개",
            order = 1,
            route = route,
        )

    private val startStation =
        createStation(
            id = "K456",
            name = "오이도",
            order = 48,
            route = route,
        )

    private val station = createStation(route = route)

    @Test
    @DisplayName("전철 도착 정보 조회 - 빈 키")
    fun testSubwayWithEmptyKeys() {
        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    subway(input: {
                        keys: []
                    }) {
                        stationID
                        name
                        order
                    }
                }
                """.trimIndent(),
                "data.subway",
            )
        assertNotNull(result)
        assertEquals(0, result.size)
    }

    @Test
    @DisplayName("전철 도착 정보 조회 (정상, 역 정보만)")
    fun testSubwayOnlyStationInfo() {
        whenever(subwayService.getStations(listOf(station.id))).thenReturn(listOf(station))
        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    subway(input: {
                        keys: [{
                            stationID: "K449",
                            direction: ["up"],
                            weekdays: ["weekdays"]
                        }],
                        limit: 10
                    }) {
                        stationID
                        name
                        order
                    }
                }
                """.trimIndent(),
                "data.subway",
            )
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("K449", result[0]["stationID"])
        assertEquals("한대앞", result[0]["name"])
        assertEquals(49, result[0]["order"])
    }

    @Test
    @DisplayName("전철 도착 정보 조회 (정상, 역 정보 + 실시간 정보 + 시간표 + 도착 정보)")
    fun testSubwayFullInfo() {
        whenever(subwayService.getStations(listOf(station.id))).thenReturn(listOf(station))
        whenever(subwayService.getRealtimeList(station.id, directions = listOf("up"))).thenReturn(
            listOf(
                createRealtime(
                    terminalStation = terminalStation,
                ),
            ),
        )
        whenever(
            subwayService.getTimetable(
                setOf(
                    SubwayTimetableKey(
                        station.id,
                        listOf("up"),
                        listOf("weekdays"),
                    ),
                ),
            ),
        ).thenReturn(
            mapOf(
                SubwayTimetableKey(station.id, listOf("up"), listOf("weekdays")) to
                    listOf(
                        createTimetable(
                            startStation = startStation,
                            terminalStation = terminalStation,
                        ),
                    ),
            ),
        )
        whenever(
            subwayService.getArrival(
                station.id,
                directions = listOf("up"),
                weekday = "weekdays",
            ),
        ).thenReturn(
            listOf(
                SubwayArrivalGroup(
                    direction = "up",
                    entries =
                        listOf(
                            SubwayArrival(
                                minutes = 2,
                                terminal =
                                    SubwayOriginTerminal(
                                        stationID = terminalStation.id,
                                        name = terminalStation.name,
                                    ),
                                isRealtime = true,
                                location = "중앙",
                                stops = 1,
                                trainNumber = "K4001",
                                isExpress = false,
                                isLast = false,
                            ),
                            SubwayArrival(
                                minutes = 25,
                                terminal =
                                    SubwayOriginTerminal(
                                        stationID = terminalStation.id,
                                        name = terminalStation.name,
                                    ),
                                isRealtime = false,
                            ),
                        ),
                ),
            ),
        )

        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    subway(input: {
                        keys: [{
                            stationID: "K449",
                            direction: ["up"],
                            weekdays: ["weekdays"]
                        }],
                        limit: 10
                    }) {
                        stationID
                        name
                        order
                        realtime {
                            location
                            minutes
                            terminal {
                                name
                            }
                        }
                        timetable {
                            time
                            origin {
                                name
                            }
                            terminal {
                                name
                            }
                        }
                        arrival {
                            direction
                            entries {
                                minutes                            
                                location
                                terminal {
                                    name
                                }
                            }
                        }
                    }
                }
                """.trimIndent(),
                "data.subway",
            )
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("K449", result[0]["stationID"])
        assertEquals("한대앞", result[0]["name"])
        assertEquals(49, result[0]["order"])
    }

    @Test
    @DisplayName("전철 도착 정보 조회 (빈 행선 필터링)")
    fun testSubwayEmptyDirectionFilter() {
        whenever(subwayService.getStations(listOf(station.id))).thenReturn(listOf(station))

        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    subway(input: {
                        keys: [{
                            stationID: "K449",
                            direction: [],
                            weekdays: ["weekdays"]
                        }],
                        limit: 10
                    }) {
                        stationID
                        name
                        order
                        realtime {
                            location
                            minutes
                            terminal {
                                name
                            }
                        }
                        arrival {
                            direction
                            entries {
                                minutes                            
                                location
                                terminal {
                                    name
                                }
                            }
                        }
                    }
                }
                """.trimIndent(),
                "data.subway",
            )
        assertNotNull(result)
        assertEquals(1, result.size)
        val station = result[0] as Map<*, *>
        val realtime = station["realtime"] as List<*>
        val arrival = station["arrival"] as List<*>
        assertEquals(0, realtime.size)
        assertEquals(0, arrival.size)
    }

    @Test
    @DisplayName("전철 도착 정보 조회 (빈 요일 필터링)")
    fun testSubwayEmptyWeekdayFilter() {
        whenever(subwayService.getStations(listOf(station.id))).thenReturn(listOf(station))

        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    subway(input: {
                        keys: [{
                            stationID: "K449",
                            direction: ["up"],
                            weekdays: []
                        }],
                        limit: 10
                    }) {
                        stationID
                        name
                        order
                        arrival {
                            direction
                            entries {
                                minutes                            
                                location
                                terminal {
                                    name
                                }
                            }
                        }
                    }
                }
                """.trimIndent(),
                "data.subway",
            )
        assertNotNull(result)
        assertEquals(1, result.size)
        val station = result[0] as Map<*, *>
        val arrival = station["arrival"] as List<*>
        assertEquals(0, arrival.size)
    }
}
