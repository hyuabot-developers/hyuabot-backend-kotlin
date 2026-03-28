package app.hyuabot.backend.bus

import app.hyuabot.backend.bus.controller.BusDataFetcher
import app.hyuabot.backend.bus.controller.BusDepartureLogDataLoader
import app.hyuabot.backend.bus.controller.BusRealtimeDataLoader
import app.hyuabot.backend.bus.controller.BusTimetableDataLoader
import app.hyuabot.backend.bus.domain.BusDepartureLogKey
import app.hyuabot.backend.bus.domain.BusTimetableKey
import app.hyuabot.backend.bus.service.BusRealtimeService
import app.hyuabot.backend.bus.service.BusRouteService
import app.hyuabot.backend.bus.service.BusStopService
import app.hyuabot.backend.bus.service.BusTimetableService
import app.hyuabot.backend.codegen.types.BusArrival
import app.hyuabot.backend.database.entity.BusDepartureLog
import app.hyuabot.backend.database.entity.BusRealtime
import app.hyuabot.backend.database.entity.BusRoute
import app.hyuabot.backend.database.entity.BusRouteStop
import app.hyuabot.backend.database.entity.BusStop
import app.hyuabot.backend.database.entity.BusTimetable
import app.hyuabot.backend.utility.ScalarRegistration
import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.test.EnableDgsTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertNotNull
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.test.Test

@EnableDgsTest
@SpringJUnitConfig
@Import(
    BusDataFetcher::class,
    BusRealtimeDataLoader::class,
    BusTimetableDataLoader::class,
    BusDepartureLogDataLoader::class,
    ScalarRegistration::class,
)
class BusDataFetcherTest {
    @Autowired lateinit var dgsQueryExecutor: DgsQueryExecutor

    @MockitoBean lateinit var routeService: BusRouteService

    @MockitoBean lateinit var stopService: BusStopService

    @MockitoBean lateinit var timetableService: BusTimetableService

    @MockitoBean lateinit var realtimeService: BusRealtimeService

    private fun now() = ZonedDateTime.now()

    private fun createBusRoute(
        id: Int = 1,
        name: String = "Test Bus Route",
        typeCode: String = "1",
        typeName: String = "Test Type",
        startStopID: Int = 1,
        endStopID: Int = 2,
        upFirstTime: LocalTime = LocalTime.parse("10:00"),
        upLastTime: LocalTime = LocalTime.parse("10:30"),
        downFirstTime: LocalTime = LocalTime.parse("10:30"),
        downLastTime: LocalTime = LocalTime.parse("11:00"),
        districtCode: Int = 0,
        companyID: Int = 1,
        companyName: String = "Test Company",
        companyPhone: String = "010-0000-0000",
    ) = BusRoute(
        id = id,
        name = name,
        typeCode = typeCode,
        typeName = typeName,
        startStopID = startStopID,
        endStopID = endStopID,
        upFirstTime = upFirstTime,
        upLastTime = upLastTime,
        downFirstTime = downFirstTime,
        downLastTime = downLastTime,
        districtCode = districtCode,
        companyID = companyID,
        companyName = companyName,
        companyPhone = companyPhone,
        stop = emptyList(),
    )

    private fun createBusStop(
        id: Int = 1,
        name: String = "Test Bus Stop",
        districtCode: Int = 0,
        mobileNumber: String = "00000",
        regionName: String = "Test Region",
        latitude: Double = 0.0,
        longitude: Double = 0.0,
    ) = BusStop(
        id = id,
        name = name,
        districtCode = districtCode,
        mobileNumber = mobileNumber,
        regionName = regionName,
        latitude = latitude,
        longitude = longitude,
        busRoutes = emptyList(),
        startBusRoutes = emptyList(),
    )

    private fun createBusRouteStop(
        seq: Int = 1,
        routeID: Int = 1,
        stopID: Int = 1,
        order: Int = 1,
        startStopID: Int = 1,
        minutes: Int = 1,
        route: BusRoute? = null,
        stop: BusStop? = null,
        startStop: BusStop? = null,
    ) = BusRouteStop(
        seq = seq,
        routeID = routeID,
        stopID = stopID,
        order = order,
        startStopID = startStopID,
        minuteFromStart = minutes,
        route = route,
        stop = stop,
        startStop = startStop,
        realtime = emptyList(),
        log = emptyList(),
    )

    private fun createBusTimetable(
        id: Int = 1,
        routeID: Int = 1,
        startStopID: Int = 1,
        weekday: String = "weekdays",
        departureTime: LocalTime = LocalTime.parse("10:00"),
    ) = BusTimetable(
        seq = id,
        routeID = routeID,
        startStopID = startStopID,
        weekday = weekday,
        departureTime = departureTime,
    )

    private fun createBusRealtime(
        routeID: Int = 1,
        stopID: Int = 1,
        order: Int = 1,
        remainingStop: Int = 1,
        remainingTime: Int = 1,
        remainingSeat: Int = 1,
        lowFloor: Boolean = false,
    ) = BusRealtime(
        routeID = routeID,
        stopID = stopID,
        order = order,
        remainingStop = remainingStop,
        remainingTime = Duration.ofMinutes(remainingTime.toLong()),
        remainingSeat = remainingSeat,
        isLowFloor = lowFloor,
        updatedAt = now(),
        routeStop = null,
    )

    private fun createBusDepartureLog(
        seq: Int = 1,
        routeID: Int = 1,
        stopID: Int = 1,
        departureDate: LocalDate = now().toLocalDate(),
        departureTime: LocalTime = now().toLocalTime(),
        vehicleID: String = "1000001",
    ) = BusDepartureLog(
        seq = seq,
        routeID = routeID,
        stopID = stopID,
        departureDate = departureDate,
        departureTime = departureTime,
        vehicleID = vehicleID,
        routeStop = null,
    )

    private val route = createBusRoute()
    private val stop = createBusStop()
    private val startStop = createBusStop(id = 2, name = "Start Stop")
    private val routeStop =
        createBusRouteStop(
            routeID = route.id,
            stopID = stop.id,
            startStopID = startStop.id,
            route = route,
            stop = stop,
            startStop = startStop,
        )

    @Test
    @DisplayName("버스 노선 정류장 조회 - 빈 배열 입력")
    fun testBusWithEmptyKeys() {
        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                query {
                    bus(input: []) {
                        order
                        minutes
                    }
                }
                """.trimIndent(),
                "data.bus",
            )
        assertNotNull(result)
        assertEquals(0, result.size)
    }

    @Test
    @DisplayName("버스 노선 정류장 조회 - 정상")
    fun testBusRouteStop() {
        whenever(routeService.fetchRouteStops(any())).thenReturn(listOf(routeStop))

        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    bus(input: [{ route: 1, order: 1, dates: ["2025-03-01"] }]) {
                        order
                        minutes
                        route {
                            seq
                            name
                            type { code name }
                            company { seq name telephone }
                            runningTime {
                                up { first last }
                                down { first last }
                            }
                        }
                        stop {
                            seq
                            name
                            districtCode
                            region
                            mobileNumber
                            latitude
                            longitude
                        }
                        startStop {
                            seq
                            name
                        }
                    }
                }
                """.trimIndent(),
                "data.bus",
            )

        assertNotNull(result)
        assertEquals(1, result.size)
        val busRouteStop = result[0]
        assertEquals(1, busRouteStop["order"])
        assertEquals(1, busRouteStop["minutes"])

        val busRoute = busRouteStop["route"] as Map<*, *>
        assertEquals(1, busRoute["seq"])
        assertEquals("Test Bus Route", busRoute["name"])

        val busStop = busRouteStop["stop"] as Map<*, *>
        assertEquals(1, busStop["seq"])
        assertEquals("Test Bus Stop", busStop["name"])
    }

    @Test
    @DisplayName("버스 실시간 도착 정보 조회 - 정상")
    fun testBusRealtime() {
        whenever(routeService.fetchRouteStops(any())).thenReturn(listOf(routeStop))
        whenever(
            realtimeService.getBusRealtimeBatch(any()),
        ).thenReturn(mapOf((route.id to stop.id) to listOf(createBusRealtime())))

        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    bus(input: [{ route: 1, order: 1, dates: ["2025-03-01"] }]) {
                        realtime {
                            order
                            stops
                            seats
                            minutes
                            lowFloor
                            updatedAt
                        }
                    }
                }
                """.trimIndent(),
                "data.bus",
            )

        assertNotNull(result)
        val realtime = result[0]["realtime"] as List<*>
        assertEquals(1, realtime.size)
        val realtimeItem = realtime[0] as Map<*, *>
        assertEquals(1, realtimeItem["order"])
        assertEquals(1, realtimeItem["stops"])
        assertEquals(1, realtimeItem["minutes"])
    }

    @Test
    @DisplayName("버스 실시간 도착 정보 조회 - 결과 없음")
    fun testBusRealtimeEmpty() {
        whenever(routeService.fetchRouteStops(any())).thenReturn(listOf(routeStop))
        whenever(
            realtimeService.getBusRealtimeBatch(any()),
        ).thenReturn(mapOf((route.id to stop.id) to emptyList()))

        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    bus(input: [{ route: 1, order: 1, dates: ["2025-03-01"] }]) {
                        realtime {
                            order
                            stops
                        }
                    }
                }
                """.trimIndent(),
                "data.bus",
            )

        assertNotNull(result)
        val realtime = result[0]["realtime"] as List<*>
        assertEquals(0, realtime.size)
    }

    @Test
    @DisplayName("버스 시간표 조회 - 정상")
    fun testBusTimetable() {
        whenever(routeService.fetchRouteStops(any())).thenReturn(listOf(routeStop))
        whenever(
            timetableService.getBusTimetableBatch(any()),
        ).thenReturn(
            mapOf(
                BusTimetableKey(routeID = route.id, startStopID = startStop.id, weekdays = null, after = null) to
                    listOf(createBusTimetable()),
            ),
        )

        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    bus(input: [{ route: 1, order: 1, dates: ["2025-03-01"] }]) {
                        timetable {
                            seq
                            weekday
                            time
                        }
                    }
                }
                """.trimIndent(),
                "data.bus",
            )

        assertNotNull(result)
        val timetable = result[0]["timetable"] as List<*>
        assertEquals(1, timetable.size)
        val timetableItem = timetable[0] as Map<*, *>
        assertEquals(1, timetableItem["seq"])
        assertEquals("weekdays", timetableItem["weekday"])
    }

    @Test
    @DisplayName("버스 시간표 조회 - 결과 없음")
    fun testBusTimetableEmpty() {
        whenever(routeService.fetchRouteStops(any())).thenReturn(listOf(routeStop))
        whenever(
            timetableService.getBusTimetableBatch(any()),
        ).thenReturn(
            mapOf(
                BusTimetableKey(routeID = route.id, startStopID = startStop.id, weekdays = null, after = null) to
                    emptyList(),
            ),
        )

        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    bus(input: [{ route: 1, order: 1, dates: ["2025-03-01"] }]) {
                        timetable {
                            seq
                            weekday
                        }
                    }
                }
                """.trimIndent(),
                "data.bus",
            )

        assertNotNull(result)
        val timetable = result[0]["timetable"] as List<*>
        assertEquals(0, timetable.size)
    }

    @Test
    @DisplayName("버스 출발 기록 조회 - 정상")
    fun testBusDepartureLog() {
        whenever(routeService.fetchRouteStops(any())).thenReturn(listOf(routeStop))
        whenever(
            routeService.getBusDepartureLogBatch(any()),
        ).thenReturn(
            mapOf(
                BusDepartureLogKey(
                    routeID = route.id,
                    stopID = stop.id,
                    dates = listOf(LocalDate.parse("2025-03-01")),
                ) to listOf(createBusDepartureLog()),
            ),
        )

        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    bus(input: [{ route: 1, order: 1, dates: ["2025-03-01"] }]) {
                        log {
                            seq
                            date
                            time
                            vehicle
                        }
                    }
                }
                """.trimIndent(),
                "data.bus",
            )

        assertNotNull(result)
        val log = result[0]["log"] as List<*>
        assertEquals(1, log.size)
        val logItem = log[0] as Map<*, *>
        assertEquals(1, logItem["seq"])
        assertEquals("1000001", logItem["vehicle"])
    }

    @Test
    @DisplayName("버스 출발 기록 조회 - datesMap에 키 없음")
    fun testBusDepartureLogNoKey() {
        val routeStopWithDifferentRoute =
            createBusRouteStop(
                routeID = 1,
                order = 1,
                route = route,
                stop = stop,
                startStop = startStop,
            )
        whenever(routeService.fetchRouteStops(any())).thenReturn(listOf(routeStopWithDifferentRoute))

        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    bus(input: [{ route: 2, order: 1, dates: ["2025-03-01"] }]) {
                        log {
                            seq
                            date
                            time
                            vehicle
                        }
                    }
                }
                """.trimIndent(),
                "data.bus",
            )

        assertNotNull(result)
        val log = result[0]["log"] as List<*>
        assertEquals(0, log.size)
    }

    @Test
    @DisplayName("버스 출발 기록 조회 - 결과 없음")
    fun testBusDepartureLogEmpty() {
        whenever(routeService.fetchRouteStops(any())).thenReturn(listOf(routeStop))
        whenever(
            routeService.getBusDepartureLogBatch(any()),
        ).thenReturn(
            mapOf(
                BusDepartureLogKey(
                    routeID = route.id,
                    stopID = stop.id,
                    dates = listOf(LocalDate.parse("2025-03-01")),
                ) to emptyList(),
            ),
        )

        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    bus(input: [{ route: 1, order: 1, dates: ["2025-03-01"] }]) {
                        log {
                            seq
                            date
                            time
                            vehicle
                        }
                    }
                }
                """.trimIndent(),
                "data.bus",
            )

        assertNotNull(result)
        val log = result[0]["log"] as List<*>
        assertEquals(0, log.size)
    }

    @Test
    @DisplayName("버스 도착 정보 조회 - 정상")
    fun testBusArrival() {
        whenever(routeService.fetchRouteStops(any())).thenReturn(listOf(routeStop))
        whenever(
            realtimeService.getArrival(
                routeID = any(),
                stopID = any(),
                startStopID = any(),
                limit = anyOrNull(),
            ),
        ).thenReturn(
            listOf(
                BusArrival(
                    stops = 1,
                    seats = 41,
                    minutes = 2,
                    lowFloor = false,
                    isRealtime = true,
                ),
                BusArrival(
                    stops = 11,
                    seats = 41,
                    minutes = 25,
                    lowFloor = false,
                    isRealtime = true,
                ),
                BusArrival(
                    stops = 21,
                    seats = 41,
                    minutes = 45,
                    lowFloor = false,
                    isRealtime = true,
                ),
                BusArrival(
                    time = LocalTime.parse("10:00"),
                    isRealtime = false,
                ),
            ),
        )

        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    bus(input: [{ route: 1, order: 1, dates: ["2025-03-01"] }]) {
                        arrival {
                            stops                        
                            seats
                            minutes
                            lowFloor
                            isRealtime
                            time
                        }
                    }
                }
                """.trimIndent(),
                "data.bus",
            )

        assertNotNull(result)
        val arrivals = result[0]["arrival"] as List<*>
        assertEquals(4, arrivals.size)
        arrivals.forEach {
            val arrivalMap = it as Map<*, *>
            if (arrivalMap["isRealtime"] == true) {
                assertNotNull(arrivalMap["stops"])
                assertNotNull(arrivalMap["minutes"])
                assertNotNull(arrivalMap["lowFloor"])
                assertNotNull(arrivalMap["seats"])
            } else {
                assertNotNull(arrivalMap["time"])
            }
        }
    }
}
