package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.CommuteShuttleRoute
import app.hyuabot.backend.database.entity.CommuteShuttleStop
import app.hyuabot.backend.database.entity.CommuteShuttleTimetable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.LocalTime

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource("classpath:application-test.properties")
class CommuteShuttleRepositoryTest {
    @Autowired private lateinit var shuttleRouteRepository: CommuteShuttleRouteRepository

    @Autowired private lateinit var shuttleStopRepository: CommuteShuttleStopRepository

    @Autowired private lateinit var shuttleTimetableRepository: CommuteShuttleTimetableRepository

    val route =
        CommuteShuttleRoute(
            name = "TEST_ROUTE_1",
            descriptionKorean = "테스트 노선 1",
            descriptionEnglish = "Test Route 1",
            timetable = listOf(),
        )

    val stops =
        listOf(
            CommuteShuttleStop(
                name = "TEST_STOP_1",
                latitude = 37.7749,
                longitude = -122.4194,
                description = "Test Stop 1",
                timetable = listOf(),
            ),
            CommuteShuttleStop(
                name = "TEST_STOP_2",
                latitude = 37.7750,
                longitude = -122.4195,
                description = "Test Stop 2",
                timetable = listOf(),
            ),
            CommuteShuttleStop(
                name = "TEST_STOP_3",
                latitude = 37.7751,
                longitude = -122.4196,
                description = "Test Stop 3",
                timetable = listOf(),
            ),
        )

    val timetables =
        listOf(
            CommuteShuttleTimetable(
                routeName = "TEST_ROUTE_1",
                stopName = "TEST_STOP_1",
                order = 0,
                departureTime = LocalTime.parse("10:00:00"),
                route = route,
                stop = stops[0],
            ),
            CommuteShuttleTimetable(
                routeName = "TEST_ROUTE_1",
                stopName = "TEST_STOP_2",
                order = 1,
                departureTime = LocalTime.parse("10:05:00"),
                route = route,
                stop = stops[1],
            ),
            CommuteShuttleTimetable(
                routeName = "TEST_ROUTE_1",
                stopName = "TEST_STOP_3",
                order = 2,
                departureTime = LocalTime.parse("10:10:00"),
                route = route,
                stop = stops[2],
            ),
        )

    @BeforeEach
    fun setUp() {
        shuttleRouteRepository.save(route)
        shuttleStopRepository.saveAllAndFlush(stops)
        shuttleTimetableRepository.saveAll(timetables)
    }

    @AfterEach
    fun tearDown() {
        shuttleTimetableRepository.deleteAll()
        shuttleStopRepository.deleteAll()
        shuttleRouteRepository.deleteAll()
    }

    @Test
    @DisplayName("통학버스 노선 키워드 검색")
    fun testFindRouteByNameContaining() {
        val foundRoutes = shuttleRouteRepository.findByNameContaining("TEST_ROUTE")
        assert(foundRoutes.isNotEmpty())
        foundRoutes.forEach {
            assert(it.name.startsWith("TEST_ROUTE"))
            assert(it.descriptionKorean.isNotEmpty())
            assert(it.descriptionEnglish.isNotEmpty())
            assert(it.timetable.isEmpty())
        }
    }

    @Test
    @DisplayName("통학버스 정류장 키워드 검색")
    fun testFindStopByNameContaining() {
        val foundStops = shuttleStopRepository.findByNameContaining("TEST_STOP")
        assert(foundStops.isNotEmpty())
        foundStops.forEach {
            assert(it.name.startsWith("TEST_STOP"))
            assert(it.latitude in -90.0..90.0)
            assert(it.longitude in -180.0..180.0)
            assert(it.description.isNotEmpty())
            assert(it.timetable.isEmpty())
        }
    }

    @Test
    @DisplayName("통학버스 노선별 시간표 조회")
    fun testFindTimetableByRouteName() {
        val foundTimetables = shuttleTimetableRepository.findByRouteName("TEST_ROUTE_1")
        assert(foundTimetables.isNotEmpty())
        foundTimetables.forEach {
            assert(it.seq != null)
            assert(it.stopName in setOf("TEST_STOP_1", "TEST_STOP_2", "TEST_STOP_3"))
            assert(it.routeName == "TEST_ROUTE_1")
            assert(it.order < 3)
            assert(it.departureTime >= LocalTime.parse("10:00:00"))
            assert(it.route.name == "TEST_ROUTE_1")
            assert(it.stop.name in setOf("TEST_STOP_1", "TEST_STOP_2", "TEST_STOP_3"))
        }
    }
}
