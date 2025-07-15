package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.BusDepartureLog
import app.hyuabot.backend.database.entity.BusRealtime
import app.hyuabot.backend.database.entity.BusRoute
import app.hyuabot.backend.database.entity.BusRouteStop
import app.hyuabot.backend.database.entity.BusStop
import app.hyuabot.backend.database.entity.BusTimetable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource("classpath:application-test.properties")
class BusRepositoryTest {
    @Autowired private lateinit var busDepartureLogRepository: BusDepartureLogRepository

    @Autowired private lateinit var busRealtimeRepository: BusRealtimeRepository

    @Autowired private lateinit var busTimetableRepository: BusTimetableRepository

    @Autowired private lateinit var busRouteStopRepository: BusRouteStopRepository

    @Autowired private lateinit var busRouteRepository: BusRouteRepository

    @Autowired private lateinit var busStopRepository: BusStopRepository

    private val stops =
        (1..10).map {
            BusStop(
                id = it,
                name = "Bus Stop $it",
                districtCode = 1,
                mobileNumber = "000${it.toString().padStart(2, '0')}",
                regionName = "경기",
                latitude = 37.7749 + it * 0.0001,
                longitude = 126.9783 + it * 0.0001,
                busRoutes = listOf(),
                startBusRoutes = listOf(),
            )
        }

    private val route =
        BusRoute(
            id = 1,
            name = "Bus Route 1",
            typeCode = "13",
            typeName = "일반시내버스",
            startStopID = 1,
            endStopID = 10,
            upFirstTime = LocalTime.parse("05:00:00"),
            downFirstTime = LocalTime.parse("05:30:00"),
            upLastTime = LocalTime.parse("23:00:00"),
            downLastTime = LocalTime.parse("23:30:00"),
            districtCode = 1,
            companyID = 1,
            companyName = "Test Bus Company",
            companyPhone = "",
            stop = listOf(),
        )

    private val routeStops =
        stops.mapIndexed { index, busStop ->
            BusRouteStop(
                routeID = 1,
                stopID = busStop.id,
                order = index,
                startStopID = 1,
                minuteFromStart = index,
                route = route,
                stop = busStop,
                startStop = stops[0],
                log = listOf(),
                realtime = listOf(),
            )
        }

    private val realtimeList =
        (1..10).map {
            BusRealtime(
                routeID = 1,
                stopID = stops[1].id,
                order = it,
                remainingStop = it * 2,
                remainingSeat = 50 - it,
                remainingTime = Duration.ofMinutes(it * 5L),
                isLowFloor = false,
                updatedAt = ZonedDateTime.now(),
                routeStop = routeStops[1],
            )
        }

    private val timetableList =
        (1..10).map {
            BusTimetable(
                routeID = 1,
                startStopID = stops[0].id,
                weekday = "weekdays",
                departureTime = LocalTime.parse("${padStartWithZero(5 + it)}:00:00"),
            )
        }

    private val logs =
        (1..100).map {
            BusDepartureLog(
                routeID = 1,
                stopID = stops[it % 10].id,
                departureDate = LocalDate.now(),
                departureTime = LocalTime.parse("05:${padStartWithZero(it % 60)}:00"),
                vehicleID = "Vehicle-${it.toString().padStart(3, '0')}",
                routeStop = routeStops[it % 10],
            )
        }

    fun padStartWithZero(
        number: Int,
        length: Int = 2,
    ): String = number.toString().padStart(length, '0')

    @BeforeEach
    fun setup() {
        busStopRepository.saveAll(stops)
        busRouteRepository.save(route)
        busRouteStopRepository.saveAll(routeStops)
        busRealtimeRepository.saveAll(realtimeList)
        busTimetableRepository.saveAll(timetableList)
        busDepartureLogRepository.saveAll(logs)
    }

    @AfterEach
    fun tearDown() {
        busDepartureLogRepository.deleteAll()
        busRealtimeRepository.deleteAll()
        busTimetableRepository.deleteAll()
        busRouteStopRepository.deleteAll()
        busRouteRepository.deleteAll()
        busStopRepository.deleteAll()
    }

    @Test
    @DisplayName("버스 정류장 목록 키워드 검색")
    fun testBusStopFindByNameContaining() {
        val foundStops = busStopRepository.findByNameContaining("Bus Stop")
        assert(foundStops.isNotEmpty())
        assert(foundStops.size == 10)
        foundStops.forEachIndexed { index, stop ->
            assert(stop.name == "Bus Stop ${index + 1}")
            assert(stop.districtCode == 1)
            assert(stop.mobileNumber == "000${(index + 1).toString().padStart(2, '0')}")
            assert(stop.regionName == "경기")
            assert(stop.latitude == 37.7749 + (index + 1) * 0.0001)
            assert(stop.longitude == 126.9783 + (index + 1) * 0.0001)
            assert(stop.busRoutes.isEmpty())
            assert(stop.startBusRoutes.isEmpty())
        }
    }

    @Test
    @DisplayName("버스 노선 목록 키워드 검색")
    fun testBusRouteFindByNameContaining() {
        val foundRoutes = busRouteRepository.findByNameContaining("Bus Route")
        assert(foundRoutes.isNotEmpty())
        assert(foundRoutes.size == 1)
        val route = foundRoutes[0]
        assert(route.id == 1)
        assert(route.name == "Bus Route 1")
        assert(route.typeCode == "13")
        assert(route.typeName == "일반시내버스")
        assert(route.startStopID == 1)
        assert(route.endStopID == 10)
        assert(route.upFirstTime == LocalTime.parse("05:00:00"))
        assert(route.downFirstTime == LocalTime.parse("05:30:00"))
        assert(route.upLastTime == LocalTime.parse("23:00:00"))
        assert(route.downLastTime == LocalTime.parse("23:30:00"))
        assert(route.districtCode == 1)
        assert(route.companyID == 1)
        assert(route.companyName == "Test Bus Company")
        assert(route.companyPhone.isEmpty())
        assert(route.stop.isEmpty())
    }

    @Test
    @DisplayName("버스 노선별 정류장 목록 조회")
    fun testBusRouteStopFindByRouteID() {
        val foundStops = busRouteStopRepository.findByRouteID(1)
        assert(foundStops.isNotEmpty())
        assert(foundStops.size == 10)
        foundStops.forEachIndexed { index, routeStop ->
            assert(routeStop.routeID == 1)
            assert(routeStop.stopID == stops[index].id)
            assert(routeStop.startStopID == stops[0].id)
            assert(routeStop.order == index)
            assert(routeStop.minuteFromStart == index)
            assert(routeStop.route.id == route.id)
            assert(routeStop.stop.id == stops[index].id)
            assert(routeStop.startStop.id == stops[0].id)
            assert(routeStop.log.isEmpty())
            assert(routeStop.realtime.isEmpty())
        }
    }

    @Test
    @DisplayName("버스 정류장별 노선 목록 조회")
    fun testBusRouteStopByStopID() {
        val foundStops = busRouteStopRepository.findByStopID(stops[0].id)
        assert(foundStops.isNotEmpty())
        assert(foundStops.size == 1)
        val routeStop = foundStops[0]
        assert(routeStop.routeID == 1)
        assert(routeStop.stopID == stops[0].id)
        assert(routeStop.order == 0)
        assert(routeStop.minuteFromStart == 0)
    }

    @Test
    @DisplayName("버스 시점 출발 시간표 조회")
    fun testBusTimetableFindByRouteIDAndStartStopID() {
        val foundTimetables =
            busTimetableRepository.findByRouteIDAndStartStopID(1, stops[0].id)
        assert(foundTimetables.isNotEmpty())
        assert(foundTimetables.size == 10)
        foundTimetables.forEachIndexed { index, timetable ->
            assert(timetable.routeID == 1)
            assert(timetable.startStopID == stops[0].id)
            assert(timetable.weekday == "weekdays")
            assert(timetable.departureTime == LocalTime.parse("${padStartWithZero(6 + index)}:00:00"))
        }
    }

    @Test
    @DisplayName("평일/주말 버스 시점 출발 시간표 조회")
    fun testBusTimetableFindByRouteIDAndStartStopIDAndWeekday() {
        val foundTimetables =
            busTimetableRepository.findByRouteIDAndStartStopIDAndWeekday(1, stops[0].id, "weekdays")
        assert(foundTimetables.isNotEmpty())
        assert(foundTimetables.size == 10)
        foundTimetables.forEachIndexed { index, timetable ->
            assert(timetable.routeID == 1)
            assert(timetable.startStopID == stops[0].id)
            assert(timetable.weekday == "weekdays")
            assert(timetable.departureTime == LocalTime.parse("${padStartWithZero(6 + index)}:00:00"))
        }
    }

    @Test
    @DisplayName("버스 실시간 정보 조회")
    fun testBusRealtimeFindByRouteIDAndStopID() {
        val foundRealtime = busRealtimeRepository.findByRouteIDAndStopID(1, stops[1].id)
        assert(foundRealtime.isNotEmpty())
        assert(foundRealtime.size == 10)
        foundRealtime.forEachIndexed { index, realtime ->
            assert(realtime.routeID == 1)
            assert(realtime.stopID == stops[1].id)
            assert(realtime.order == index + 1)
            assert(realtime.remainingStop == (index + 1) * 2)
            assert(realtime.remainingSeat == 50 - (index + 1))
            assert(realtime.remainingTime == Duration.ofMinutes((index + 1) * 5L))
            assert(!realtime.isLowFloor)
            assert(realtime.updatedAt.isBefore(ZonedDateTime.now()))
            assert(realtime.routeStop.routeID == realtime.routeID)
            assert(realtime.routeStop.stopID == realtime.stopID)
        }
    }

    @Test
    @DisplayName("정류장별 버스 출발 기록 조회")
    fun testBusDepartureLogFindByRouteIDAndStopID() {
        val foundLogs = busDepartureLogRepository.findByRouteIDAndStopID(1, stops[0].id)
        assert(foundLogs.isNotEmpty())
        foundLogs.forEach { log ->
            assert(log.routeID == 1)
            assert(log.stopID == stops[0].id)
            assert(log.departureDate == LocalDate.now())
            assert(log.departureTime >= LocalTime.parse("05:00:00"))
            assert(log.vehicleID.startsWith("Vehicle-"))
            assert(log.routeStop.routeID == log.routeID)
            assert(log.routeStop.stopID == log.stopID)
        }
    }

    @Test
    @DisplayName("정류장 및 출발 날짜별 버스 출발 기록 조회")
    fun testBusDepartureLogFindByRouteIDAndStopIDAndDepartureDate() {
        val foundLogs = busDepartureLogRepository.findByRouteIDAndStopIDAndDepartureDate(1, stops[0].id, LocalDate.now())
        assert(foundLogs.isNotEmpty())
        foundLogs.forEach { log ->
            assert(log.routeID == 1)
            assert(log.stopID == stops[0].id)
            assert(log.departureDate == LocalDate.now())
            assert(log.departureTime >= LocalTime.parse("05:00:00"))
            assert(log.vehicleID.startsWith("Vehicle-"))
            assert(log.routeStop.routeID == log.routeID)
            assert(log.routeStop.stopID == log.stopID)
        }
    }
}
