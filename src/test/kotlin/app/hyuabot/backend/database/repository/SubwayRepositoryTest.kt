package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.SubwayRealtime
import app.hyuabot.backend.database.entity.SubwayRoute
import app.hyuabot.backend.database.entity.SubwayRouteStation
import app.hyuabot.backend.database.entity.SubwayStation
import app.hyuabot.backend.database.entity.SubwayTimetable
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
import java.time.LocalTime
import java.time.ZonedDateTime

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource("classpath:application-test.properties")
class SubwayRepositoryTest {
    @Autowired private lateinit var stationNameRepository: SubwayStationNameRepository

    @Autowired private lateinit var routeRepository: SubwayRouteRepository

    @Autowired private lateinit var stationRepository: SubwayStationRepository

    @Autowired private lateinit var realtimeRepository: SubwayRealtimeRepository

    @Autowired private lateinit var timetableRepository: SubwayTimetableRepository

    val stationName = (1..10).map { SubwayStation("STATION$it", emptyList()) }
    val route = SubwayRoute(1001, "1호선", emptyList())
    val station =
        (1..10).map {
            SubwayRouteStation(
                "K$it",
                1001,
                "STATION$it",
                it,
                Duration.ofMinutes(it.toLong()),
                route,
                stationName[it - 1],
                emptyList(),
                emptyList(),
            )
        }
    val realtime =
        (1..100).map {
            SubwayRealtime(
                "K1",
                heading = listOf("up", "down").get(it % 2),
                order = it / 2,
                location = "중앙",
                remainingStop = it / 2,
                remainingTime = Duration.ofMinutes(it.toLong()),
                terminalStationID = station[station.size - 1].id,
                trainNumber = "K001",
                updatedAt = ZonedDateTime.now(),
                isExpress = false,
                isLast = false,
                status = 0,
                station = station[0],
                terminalStation = station[station.size - 1],
            )
        }
    val timetable =
        (1..9).map {
            SubwayTimetable(
                "K1",
                "K1",
                terminalStationID = "K10",
                departureTime = LocalTime.parse("09:0$it:00"),
                weekday = "weekdays",
                heading = listOf("up", "down").get(it % 2),
                station = station[0],
                startStation = station[0],
                terminalStation = station[station.size - 1],
            )
        }

    @BeforeEach
    fun setup() {
        stationNameRepository.saveAll(stationName)
        routeRepository.save(route)
        stationRepository.saveAll(station)
        realtimeRepository.saveAll(realtime)
        timetableRepository.saveAll(timetable)
    }

    @AfterEach
    fun tearDown() {
        timetableRepository.deleteAll()
        realtimeRepository.deleteAll()
        stationRepository.deleteAll()
        routeRepository.deleteAll()
        stationNameRepository.deleteAll()
    }

    @Test
    @DisplayName("지하철역 이름 키워드 검색")
    fun testStationNameFindByNameContaining() {
        val found = stationNameRepository.findByNameContaining("STATION")
        assert(found.isNotEmpty())
        found.forEach {
            assert(it.name.startsWith("STATION"))
            assert(it.subwayLine.isEmpty())
        }
    }

    @Test
    @DisplayName("지하철 노선 이름 키워드 검색")
    fun testRouteFindByNameContaining() {
        val found = routeRepository.findByNameContaining("1호선")
        assert(found.isNotEmpty())
        found.forEach {
            assert(it.id == 1001)
            assert(it.name == "1호선")
            assert(it.station.isEmpty())
        }
    }

    @Test
    @DisplayName("지하철역 키워드 검색")
    fun testStationFindByNameContaining() {
        val found = stationRepository.findByNameContaining("STATION")
        assert(found.isNotEmpty())
        found.forEach {
            assert(it.id.startsWith("K"))
            assert(it.name.startsWith("STATION"))
            assert(it.routeID == 1001)
            assert(it.route.name == "1호선")
            assert(it.order >= 1 && it.order <= 10)
            assert(it.cumulativeTime.toMinutes() == it.order.toLong())
            assert(it.stationName.name.startsWith("STATION"))
            assert(it.realtime.isEmpty())
            assert(it.timetable.isEmpty())
        }
    }

    @Test
    @DisplayName("노선별 지하철역 목록 조회")
    fun testStationFindByRouteID() {
        val found = stationRepository.findByRouteID(1001)
        assert(found.isNotEmpty())
        found.forEach {
            assert(it.routeID == 1001)
            assert(it.route.name == "1호선")
            assert(it.stationName.name.startsWith("STATION"))
        }
    }

    @Test
    @DisplayName("지하철역 시간표 조회")
    fun testTimetableFindByStationID() {
        val found = timetableRepository.findByStationID("K1")
        assert(found.isNotEmpty())
        found.forEach {
            assert(it.station.id == "K1")
            assert(it.startStationID == "K1")
            assert(it.terminalStationID == "K10")
            assert(it.startStation.id == "K1")
            assert(it.terminalStation.id == "K10")
            assert(it.departureTime >= LocalTime.parse("09:00:00"))
            assert(it.weekday == "weekdays")
            assert(it.heading in listOf("up", "down"))
        }
    }

    @Test
    @DisplayName("지하철역 시간표 조회(출발 예정)")
    fun testTimetableFindByStationIDAndDepartureTimeAfter() {
        val found = timetableRepository.findByStationIDAndDepartureTimeAfter("K1", LocalTime.parse("09:05:00"))
        assert(found.isNotEmpty())
        found.forEach {
            assert(it.station.id == "K1")
            assert(it.startStationID == "K1")
            assert(it.terminalStationID == "K10")
            assert(it.startStation.id == "K1")
            assert(it.terminalStation.id == "K10")
            assert(it.departureTime > LocalTime.parse("09:05:00"))
            assert(it.weekday == "weekdays")
            assert(it.heading in listOf("up", "down"))
        }
    }

    @Test
    @DisplayName("지하철역 시간표 조회(방향별)")
    fun testTimetableFindByStationIDAndHeading() {
        val found = timetableRepository.findByStationIDAndHeading("K1", "up")
        assert(found.isNotEmpty())
        found.forEach {
            assert(it.station.id == "K1")
            assert(it.startStationID == "K1")
            assert(it.terminalStationID == "K10")
            assert(it.startStation.id == "K1")
            assert(it.terminalStation.id == "K10")
            assert(it.heading == "up")
            assert(it.weekday == "weekdays")
        }
    }

    @Test
    @DisplayName("지하철역 시간표 조회(방향별/출발 예정)")
    fun testTimetableFindByStationIDAndDepartureTimeAfterAndHeading() {
        val found =
            timetableRepository.findByStationIDAndDepartureTimeAfterAndHeading(
                "K1",
                LocalTime.parse("09:05:00"),
                "up",
            )
        assert(found.isNotEmpty())
        found.forEach {
            assert(it.station.id == "K1")
            assert(it.startStationID == "K1")
            assert(it.terminalStationID == "K10")
            assert(it.startStation.id == "K1")
            assert(it.terminalStation.id == "K10")
            assert(it.departureTime > LocalTime.parse("09:05:00"))
            assert(it.heading == "up")
            assert(it.weekday == "weekdays")
        }
    }

    @Test
    @DisplayName("지하철역 시간표 조회(요일별)")
    fun testTimetableFindByStationIDAndWeekday() {
        val found = timetableRepository.findByStationIDAndWeekday("K1", "weekdays")
        assert(found.isNotEmpty())
        found.forEach {
            assert(it.station.id == "K1")
            assert(it.startStationID == "K1")
            assert(it.terminalStationID == "K10")
            assert(it.startStation.id == "K1")
            assert(it.terminalStation.id == "K10")
            assert(it.weekday == "weekdays")
        }
    }

    @Test
    @DisplayName("지하철역 시간표 조회(요일별/출발 예정)")
    fun testTimetableFindByStationIDAndWeekdayAndDepartureTimeAfter() {
        val found =
            timetableRepository.findByStationIDAndWeekdayAndDepartureTimeAfter(
                "K1",
                "weekdays",
                LocalTime.parse("09:05:00"),
            )
        assert(found.isNotEmpty())
        found.forEach {
            assert(it.station.id == "K1")
            assert(it.startStationID == "K1")
            assert(it.terminalStationID == "K10")
            assert(it.startStation.id == "K1")
            assert(it.terminalStation.id == "K10")
            assert(it.weekday == "weekdays")
            assert(it.departureTime > LocalTime.parse("09:05:00"))
        }
    }

    @Test
    @DisplayName("지하철역 시간표 조회(방향별/요일별)")
    fun testTimetableFindByStationIDAndHeadingAndWeekday() {
        val found = timetableRepository.findByStationIDAndHeadingAndWeekday("K1", "up", "weekdays")
        assert(found.isNotEmpty())
        found.forEach {
            assert(it.station.id == "K1")
            assert(it.startStationID == "K1")
            assert(it.terminalStationID == "K10")
            assert(it.startStation.id == "K1")
            assert(it.terminalStation.id == "K10")
            assert(it.heading == "up")
            assert(it.weekday == "weekdays")
        }
    }

    @Test
    @DisplayName("지하철역 시간표 조회(방향별/요일별/출발 예정)")
    fun testTimetableFindByStationIDAndHeadingAndWeekdayAndDepartureTimeAfter() {
        val found =
            timetableRepository.findByStationIDAndHeadingAndWeekdayAndDepartureTimeAfter(
                "K1",
                "up",
                "weekdays",
                LocalTime.parse("09:05:00"),
            )
        assert(found.isNotEmpty())
        found.forEach {
            assert(it.station.id == "K1")
            assert(it.startStationID == "K1")
            assert(it.terminalStationID == "K10")
            assert(it.startStation.id == "K1")
            assert(it.terminalStation.id == "K10")
            assert(it.heading == "up")
            assert(it.weekday == "weekdays")
            assert(it.departureTime > LocalTime.parse("09:05:00"))
        }
    }

    @Test
    @DisplayName("지하철 실시간 정보 조회")
    fun testRealtimeFindByStationID() {
        val found = realtimeRepository.findByStationID("K1")
        assert(found.isNotEmpty())
        found.forEach {
            assert(it.station.id == "K1")
            assert(it.heading in listOf("up", "down"))
            assert(it.order >= 0)
            assert(it.location == "중앙")
            assert(it.remainingStop >= 0)
            assert(it.remainingTime.toMinutes() >= 0)
            assert(it.terminalStationID == "K10")
            assert(it.terminalStation.name == "STATION10")
            assert(it.trainNumber.startsWith("K"))
            assert(it.updatedAt.isBefore(ZonedDateTime.now()))
            assert(!it.isExpress)
            assert(!it.isLast)
            assert(it.status == 0)
        }
    }

    @Test
    @DisplayName("지하철 실시간 정보 조회(방향별)")
    fun testRealtimeFindByStationIDAndHeading() {
        val found = realtimeRepository.findByStationIDAndHeading("K1", "up")
        assert(found.isNotEmpty())
        found.forEach {
            assert(it.station.id == "K1")
            assert(it.heading == "up")
            assert(it.order >= 0)
            assert(it.location == "중앙")
            assert(it.remainingStop >= 0)
            assert(it.remainingTime.toMinutes() >= 0)
            assert(it.terminalStationID == "K10")
            assert(it.terminalStation.name == "STATION10")
            assert(it.trainNumber.startsWith("K"))
            assert(it.updatedAt.isBefore(ZonedDateTime.now()))
            assert(!it.isExpress)
            assert(!it.isLast)
            assert(it.status == 0)
        }
    }
}
