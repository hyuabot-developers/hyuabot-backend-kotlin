package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.ShuttleHoliday
import app.hyuabot.backend.database.entity.ShuttlePeriod
import app.hyuabot.backend.database.entity.ShuttlePeriodType
import app.hyuabot.backend.database.entity.ShuttleRoute
import app.hyuabot.backend.database.entity.ShuttleRouteStop
import app.hyuabot.backend.database.entity.ShuttleStop
import app.hyuabot.backend.database.entity.ShuttleTimetable
import app.hyuabot.backend.database.entity.ShuttleTimetableView
import jakarta.persistence.EntityManager
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
class ShuttleRepositoryTest {
    @Autowired lateinit var holidayRepository: ShuttleHolidayRepository

    @Autowired lateinit var periodTypeRepository: ShuttlePeriodTypeRepository

    @Autowired lateinit var periodRepository: ShuttlePeriodRepository

    @Autowired lateinit var routeRepository: ShuttleRouteRepository

    @Autowired lateinit var stopRepository: ShuttleStopRepository

    @Autowired lateinit var routeStopRepository: ShuttleRouteStopRepository

    @Autowired lateinit var timetableRepository: ShuttleTimetableRepository

    @Autowired lateinit var timetableViewRepository: ShuttleTimetableViewRepository

    @Autowired lateinit var entityManager: EntityManager

    private val holidays =
        listOf(
            ShuttleHoliday(date = LocalDate.of(2025, 1, 1), type = "halt", calendarType = "solar"),
            ShuttleHoliday(date = LocalDate.of(2025, 1, 1), type = "halt", calendarType = "lunar"),
            ShuttleHoliday(date = LocalDate.of(2025, 8, 15), type = "weekends", calendarType = "solar"),
        )

    private val periodTypes =
        listOf(
            ShuttlePeriodType("semester", listOf()),
            ShuttlePeriodType("vacation_session", listOf()),
            ShuttlePeriodType("vacation", listOf()),
        )

    private val periods =
        listOf(
            ShuttlePeriod(
                type = "semester",
                start = ZonedDateTime.parse("2025-03-02T00:00:00+09:00"),
                end = ZonedDateTime.parse("2025-06-24T23:59:59+09:00"),
                periodType = periodTypes[0],
            ),
            ShuttlePeriod(
                type = "vacation_session",
                start = ZonedDateTime.parse("2025-06-25T00:00:00+09:00"),
                end = ZonedDateTime.parse("2025-07-15T23:59:59+09:00"),
                periodType = periodTypes[1],
            ),
            ShuttlePeriod(
                type = "vacation",
                start = ZonedDateTime.parse("2025-07-16T00:00:00+09:00"),
                end = ZonedDateTime.parse("2025-08-31T23:59:59+09:00"),
                periodType = periodTypes[2],
            ),
            ShuttlePeriod(
                type = "semester",
                start = ZonedDateTime.parse("2025-09-01T00:00:00+09:00"),
                end = ZonedDateTime.parse("2025-12-23T23:59:59+09:00"),
                periodType = periodTypes[0],
            ),
            ShuttlePeriod(
                type = "vacation_session",
                start = ZonedDateTime.parse("2025-12-24T00:00:00+09:00"),
                end = ZonedDateTime.parse("2026-01-14T23:59:59+09:00"),
                periodType = periodTypes[1],
            ),
            ShuttlePeriod(
                type = "vacation",
                start = ZonedDateTime.parse("2026-01-15T00:00:00+09:00"),
                end = ZonedDateTime.parse("2026-03-01T23:59:59+09:00"),
                periodType = periodTypes[2],
            ),
        )
    private val stops =
        listOf(
            ShuttleStop(
                "dormitory_o",
                37.5665,
                126.978,
                listOf(),
                listOf(),
                listOf(),
            ),
            ShuttleStop(
                "shuttlecock_o",
                37.5666,
                126.979,
                listOf(),
                listOf(),
                listOf(),
            ),
            ShuttleStop(
                "dormitory_i",
                37.5667,
                126.980,
                listOf(),
                listOf(),
                listOf(),
            ),
        )
    private val route =
        ShuttleRoute(
            "CDD",
            "순환(기숙사~기숙사)",
            "Circular (Dormitory to Dormitory)",
            "C",
            "dormitory_o",
            "dormitory_i",
            listOf(),
            listOf(),
            stops[0],
            stops[2],
        )
    private val routeStops =
        listOf(
            ShuttleRouteStop(
                routeName = "CDD",
                stopName = "dormitory_o",
                order = 0,
                cumulativeTime = Duration.ofMinutes(-5),
                route = route,
                stop = stops[0],
            ),
            ShuttleRouteStop(
                routeName = "CDD",
                stopName = "shuttlecock_o",
                order = 1,
                cumulativeTime = Duration.ofMinutes(0),
                route = route,
                stop = stops[1],
            ),
            ShuttleRouteStop(
                routeName = "CDD",
                stopName = "dormitory_i",
                order = 2,
                cumulativeTime = Duration.ofMinutes(5),
                route = route,
                stop = stops[2],
            ),
        )
    private val timetable =
        listOf(
            ShuttleTimetable(
                periodType = "semester",
                weekday = true,
                routeName = "CDD",
                departureTime = LocalTime.parse("08:00:00"),
                route = null,
            ),
            ShuttleTimetable(
                periodType = "semester",
                weekday = true,
                routeName = "CDD",
                departureTime = LocalTime.parse("08:30:00"),
                route = null,
            ),
            ShuttleTimetable(
                periodType = "semester",
                weekday = true,
                routeName = "CDD",
                departureTime = LocalTime.parse("09:00:00"),
                route = null,
            ),
        )

    @BeforeEach
    fun setUp() {
        holidayRepository.saveAll(holidays)
        periodTypeRepository.saveAllAndFlush(periodTypes)
        periodRepository.saveAllAndFlush(periods)
        stopRepository.saveAllAndFlush(stops)
        routeRepository.saveAndFlush(route)
        routeStopRepository.saveAll(routeStops)
        timetableRepository.saveAll(timetable)
        entityManager
            .createNativeQuery(
                "REFRESH MATERIALIZED VIEW shuttle_timetable_grouped_view",
            ).executeUpdate()
    }

    @AfterEach
    fun tearDown() {
        timetableRepository.deleteAll()
        routeStopRepository.deleteAll()
        routeRepository.deleteAll()
        stopRepository.deleteAll()
        periodRepository.deleteAll()
        periodTypeRepository.deleteAll()
        holidayRepository.deleteAll()
    }

    @Test
    @DisplayName("음력/양력 및 날짜로 휴일 조회")
    fun testHolidayFindByDateAndCalendarType() {
        val date = LocalDate.of(2025, 1, 1)
        val calendarType = "solar"
        val holidays = holidayRepository.findByDateAndCalendarType(date, calendarType)
        assert(holidays != null)
        holidays?.let {
            assert(it.seq != null)
            assert(listOf("halt", "weekends").contains(it.type))
            assert(it.date == date)
            assert(it.calendarType == calendarType)
        }
    }

    @Test
    @DisplayName("현재 시간 기준으로 학기/계절학기/방학 기간 조회")
    fun testCurrentPeriods() {
        val currentTime = ZonedDateTime.parse("2025-03-15T12:00:00+09:00")
        val currentPeriods = periodRepository.findByStartBeforeAndEndAfter(currentTime, currentTime)
        assert(currentPeriods != null)
        currentPeriods?.let {
            assert(it.seq != null)
            assert(it.start.isBefore(currentTime))
            assert(it.end.isAfter(currentTime))
            assert(it.type == "semester")
            assert(it.periodType?.type == "semester")
            assert(it.periodType?.period?.isEmpty() == true)
        }
    }

    @Test
    @DisplayName("셔틀버스 노선 키워드 검색")
    fun testRouteFindByNameContaining() {
        val keyword = "CDD"
        val routes = routeRepository.findByNameContaining(keyword)
        assert(routes.isNotEmpty())
        routes.forEach { route ->
            assert(route.name.contains(keyword))
            assert(route.descriptionKorean == "순환(기숙사~기숙사)")
            assert(route.descriptionEnglish == "Circular (Dormitory to Dormitory)")
            assert(route.tag == "C")
            assert(route.startStopID == "dormitory_o")
            assert(route.endStopID == "dormitory_i")
            assert(route.startStop.name == "dormitory_o")
            assert(route.endStop.name == "dormitory_i")
            assert(route.timetable.isEmpty())
            assert(route.stop.isEmpty())
        }
    }

    @Test
    @DisplayName("셔틀버스 정류장 키워드 검색")
    fun testStopFindByNameContaining() {
        val keyword = "dormitory"
        val stops = stopRepository.findByNameContaining(keyword)
        assert(stops.isNotEmpty())
        stops.forEach { stop ->
            assert(stop.name.contains(keyword))
            assert(stop.latitude == 37.5665 || stop.latitude == 37.5667)
            assert(stop.longitude == 126.978 || stop.longitude == 126.980)
            assert(stop.route.isEmpty())
            assert(stop.routeToStart.isEmpty())
            assert(stop.routeToEnd.isEmpty())
        }
    }

    @Test
    @DisplayName("셔틀버스 노선별 정류장 조회")
    fun testRouteStopFindByRouteName() {
        val routeName = "CDD"
        val routeStops = routeStopRepository.findByRouteName(routeName)
        assert(routeStops.isNotEmpty())
        routeStops.forEach { routeStop ->
            assert(routeStop.seq != null)
            assert(routeStop.stopName in listOf("dormitory_o", "shuttlecock_o", "dormitory_i"))
            assert(routeStop.routeName == routeName)
            assert(routeStop.stop.name in listOf("dormitory_o", "shuttlecock_o", "dormitory_i"))
            assert(routeStop.order in 0..2)
            assert(
                routeStop.cumulativeTime == Duration.ofMinutes(-5) ||
                    routeStop.cumulativeTime == Duration.ofMinutes(0) ||
                    routeStop.cumulativeTime == Duration.ofMinutes(5),
            )
            assert(routeStop.route.name == "CDD")
            assert(routeStop.stop.route.isEmpty())
            assert(routeStop.stop.routeToStart.isEmpty())
            assert(routeStop.stop.routeToEnd.isEmpty())
        }
    }

    @Test
    @DisplayName("셔틀버스 정류장별 경유 노선 조회")
    fun testRouteStopFindByStopName() {
        val stopName = "dormitory_o"
        val routeStops = routeStopRepository.findByStopName(stopName)
        assert(routeStops.isNotEmpty())
        routeStops.forEach { routeStop ->
            assert(routeStop.stop.name == stopName)
            assert(routeStop.route.name == "CDD")
            assert(routeStop.order == 0)
            assert(routeStop.cumulativeTime == Duration.ofMinutes(-5))
            assert(routeStop.route.startStop.name == "dormitory_o")
            assert(routeStop.route.endStop.name == "dormitory_i")
        }
    }

    @Test
    @DisplayName("셔틀버스 시간표 조회(학기/계절학기/방학)")
    fun testTimetableFindByPeriodType() {
        val periodType = "semester"
        val timetables = timetableRepository.findByPeriodType(periodType)
        assert(timetables.isNotEmpty())
        timetables.forEach { timetable ->
            assert(timetable.seq != null)
            assert(timetable.periodType == periodType)
            assert(timetable.weekday)
            assert(timetable.routeName == "CDD")
            assert(
                timetable.departureTime == LocalTime.parse("08:00:00") ||
                    timetable.departureTime == LocalTime.parse("08:30:00") ||
                    timetable.departureTime == LocalTime.parse("09:00:00"),
            )
            assert(timetable.route == null)
        }
    }

    @Test
    @DisplayName("셔틀버스 시간표 조회(학기/계절학기/방학+평일/주말)")
    fun testTimetableFindByPeriodTypeAndWeekday() {
        val periodType = "semester"
        val weekday = true
        val timetables = timetableRepository.findByPeriodTypeAndWeekday(periodType, weekday)
        assert(timetables.isNotEmpty())
        timetables.forEach { timetable ->
            assert(timetable.seq != null)
            assert(timetable.periodType == periodType)
            assert(timetable.weekday == weekday)
            assert(timetable.routeName == "CDD")
            assert(
                timetable.departureTime == LocalTime.parse("08:00:00") ||
                    timetable.departureTime == LocalTime.parse("08:30:00") ||
                    timetable.departureTime == LocalTime.parse("09:00:00"),
            )
            assert(timetable.route == null)
        }
    }

    @Test
    @DisplayName("셔틀버스 시간표 조회(학기/계절학기/방학+노선명)")
    fun testTimetableFindByPeriodTypeAndRouteName() {
        val periodType = "semester"
        val routeName = "CDD"
        val timetables = timetableRepository.findByPeriodTypeAndRouteName(periodType, routeName)
        assert(timetables.isNotEmpty())
        timetables.forEach { timetable ->
            assert(timetable.seq != null)
            assert(timetable.periodType == periodType)
            assert(timetable.weekday)
            assert(timetable.routeName == routeName)
            assert(
                timetable.departureTime == LocalTime.parse("08:00:00") ||
                    timetable.departureTime == LocalTime.parse("08:30:00") ||
                    timetable.departureTime == LocalTime.parse("09:00:00"),
            )
            assert(timetable.route == null)
        }
    }

    @Test
    @DisplayName("셔틀버스 시간표 조회(학기/계절학기/방학+노선명+평일/주말)")
    fun testTimetableFindByPeriodTypeAndRouteNameAndWeekday() {
        val periodType = "semester"
        val routeName = "CDD"
        val weekday = true
        val timetables = timetableRepository.findByPeriodTypeAndRouteNameAndWeekday(periodType, routeName, weekday)
        assert(timetables.isNotEmpty())
        timetables.forEach { timetable ->
            assert(timetable.seq != null)
            assert(timetable.periodType == periodType)
            assert(timetable.weekday == weekday)
            assert(timetable.routeName == routeName)
            assert(
                timetable.departureTime == LocalTime.parse("08:00:00") ||
                    timetable.departureTime == LocalTime.parse("08:30:00") ||
                    timetable.departureTime == LocalTime.parse("09:00:00"),
            )
            assert(timetable.route == null)
        }
    }

    @Test
    @DisplayName("셔틀버스 시간표 생성(읽기 전용 뷰)")
    fun testTimetableViewItemInit() {
        // Test raise error if try to insert into timetable view
        val item =
            ShuttleTimetableView(
                seq = 1,
                periodType = "semester",
                weekday = true,
                routeName = "CDD",
                routeTag = "C",
                stopName = "dormitory_o",
                departureTime = LocalTime.parse("08:00:00"),
                destinationGroup = "STATION",
            )
        assert(item.seq == 1)
        assert(item.periodType == "semester")
        assert(item.weekday)
        assert(item.routeName == "CDD")
        assert(item.routeTag == "C")
        assert(item.stopName == "dormitory_o")
        assert(item.departureTime == LocalTime.parse("08:00:00"))
        assert(item.destinationGroup == "STATION")
    }

    @Test
    @DisplayName("셔틀버스 시간표 뷰 조회(학기/계절학기/방학)")
    fun testTimetableViewFindByPeriodType() {
        val periodType = "semester"
        val timetables = timetableViewRepository.findByPeriodType(periodType)
        assert(timetables.isNotEmpty())
        timetables.forEach { timetable ->
            assert(timetable.periodType == periodType)
            assert(timetable.weekday)
            assert(timetable.routeName == "CDD")
            assert(timetable.routeTag == "C")
            assert(
                timetable.departureTime >= LocalTime.parse("07:55:00"),
            )
            assert(
                timetable.destinationGroup == "STATION" ||
                    timetable.destinationGroup == "TERMINAL" ||
                    timetable.destinationGroup == "CAMPUS",
            )
        }
    }

    @Test
    @DisplayName("셔틀버스 시간표 뷰 조회(학기/계절학기/방학+평일/주말)")
    fun testTimetableViewFindByPeriodTypeAndWeekday() {
        val periodType = "semester"
        val isWeekdays = true
        val timetables = timetableViewRepository.findByPeriodTypeAndWeekday(periodType, isWeekdays)
        assert(timetables.isNotEmpty())
        timetables.forEach { timetable ->
            assert(timetable.periodType == periodType)
            assert(timetable.weekday == isWeekdays)
            assert(timetable.routeName == "CDD")
            assert(timetable.routeTag == "C")
            assert(
                timetable.departureTime >= LocalTime.parse("07:55:00"),
            )
            assert(
                timetable.destinationGroup == "STATION" ||
                    timetable.destinationGroup == "TERMINAL" ||
                    timetable.destinationGroup == "CAMPUS",
            )
        }
    }

    @Test
    @DisplayName("셔틀버스 시간표 뷰 조회(학기/계절학기/방학+노선명)")
    fun testTimetableViewFindByPeriodTypeAndRouteName() {
        val periodType = "semester"
        val routeName = "CDD"
        val timetables = timetableViewRepository.findByPeriodTypeAndRouteName(periodType, routeName)
        assert(timetables.isNotEmpty())
        timetables.forEach { timetable ->
            assert(timetable.periodType == periodType)
            assert(timetable.weekday)
            assert(timetable.routeName == routeName)
            assert(timetable.routeTag == "C")
            assert(
                timetable.departureTime >= LocalTime.parse("07:55:00"),
            )
            assert(
                timetable.destinationGroup == "STATION" ||
                    timetable.destinationGroup == "TERMINAL" ||
                    timetable.destinationGroup == "CAMPUS",
            )
        }
    }

    @Test
    @DisplayName("셔틀버스 시간표 뷰 조회(학기/계절학기/방학+노선 태그+평일/주말)")
    fun testTimetableViewFindByPeriodTypeAndRouteTag() {
        val periodType = "semester"
        val routeTag = "C"
        val timetables = timetableViewRepository.findByPeriodTypeAndRouteTag(periodType, routeTag)
        assert(timetables.isNotEmpty())
        timetables.forEach { timetable ->
            assert(timetable.periodType == periodType)
            assert(timetable.weekday)
            assert(timetable.routeName == "CDD")
            assert(timetable.routeTag == routeTag)
            assert(
                timetable.departureTime >= LocalTime.parse("07:55:00"),
            )
            assert(
                timetable.destinationGroup == "STATION" ||
                    timetable.destinationGroup == "TERMINAL" ||
                    timetable.destinationGroup == "CAMPUS",
            )
        }
    }

    @Test
    @DisplayName("셔틀버스 시간표 뷰 조회(학기/계절학기/방학+정류장명)")
    fun testTimetableViewFindByPeriodTypeAndStopName() {
        val periodType = "semester"
        val stopName = "dormitory_o"
        val timetables = timetableViewRepository.findByPeriodTypeAndStopName(periodType, stopName)
        assert(timetables.isNotEmpty())
        timetables.forEach { timetable ->
            assert(timetable.periodType == periodType)
            assert(timetable.weekday)
            assert(timetable.routeName == "CDD")
            assert(timetable.routeTag == "C")
            assert(timetable.stopName == stopName)
            assert(
                timetable.departureTime >= LocalTime.parse("07:55:00"),
            )
            assert(
                timetable.destinationGroup == "STATION" ||
                    timetable.destinationGroup == "TERMINAL" ||
                    timetable.destinationGroup == "CAMPUS",
            )
        }
    }

    @Test
    @DisplayName("셔틀버스 시간표 뷰 조회(학기/계절학기/방학+노선명+평일/주말)")
    fun testTimetableViewFindByPeriodTypeAndRouteNameAndWeekday() {
        val periodType = "semester"
        val routeName = "CDD"
        val isWeekdays = true
        val timetables =
            timetableViewRepository.findByPeriodTypeAndRouteNameAndWeekday(periodType, routeName, isWeekdays)
        assert(timetables.isNotEmpty())
        timetables.forEach { timetable ->
            assert(timetable.periodType == periodType)
            assert(timetable.weekday == isWeekdays)
            assert(timetable.routeName == routeName)
            assert(timetable.routeTag == "C")
            assert(
                timetable.departureTime >= LocalTime.parse("07:55:00"),
            )
            assert(
                timetable.destinationGroup == "STATION" ||
                    timetable.destinationGroup == "TERMINAL" ||
                    timetable.destinationGroup == "CAMPUS",
            )
        }
    }

    @Test
    @DisplayName("셔틀버스 시간표 뷰 조회(학기/계절학기/방학+노선 태그+평일/주말)")
    fun testTimetableViewFindByPeriodTypeAndRouteTagAndWeekday() {
        val periodType = "semester"
        val routeTag = "C"
        val isWeekdays = true
        val timetables =
            timetableViewRepository.findByPeriodTypeAndRouteTagAndWeekday(periodType, routeTag, isWeekdays)
        assert(timetables.isNotEmpty())
        timetables.forEach { timetable ->
            assert(timetable.periodType == periodType)
            assert(timetable.weekday == isWeekdays)
            assert(timetable.routeName == "CDD")
            assert(timetable.routeTag == routeTag)
            assert(
                timetable.departureTime >= LocalTime.parse("07:55:00"),
            )
            assert(
                timetable.destinationGroup == "STATION" ||
                    timetable.destinationGroup == "TERMINAL" ||
                    timetable.destinationGroup == "CAMPUS",
            )
        }
    }

    @Test
    @DisplayName("셔틀버스 시간표 뷰 조회(학기/계절학기/방학+정류장명+평일/주말)")
    fun testTimetableViewFindByPeriodTypeAndStopNameAndWeekday() {
        val periodType = "semester"
        val stopName = "dormitory_o"
        val isWeekdays = true
        val timetables =
            timetableViewRepository.findByPeriodTypeAndStopNameAndWeekday(periodType, stopName, isWeekdays)
        assert(timetables.isNotEmpty())
        timetables.forEach { timetable ->
            assert(timetable.periodType == periodType)
            assert(timetable.weekday == isWeekdays)
            assert(timetable.routeName == "CDD")
            assert(timetable.routeTag == "C")
            assert(timetable.stopName == stopName)
            assert(
                timetable.departureTime >= LocalTime.parse("07:55:00"),
            )
            assert(
                timetable.destinationGroup == "STATION" ||
                    timetable.destinationGroup == "TERMINAL" ||
                    timetable.destinationGroup == "CAMPUS",
            )
        }
    }
}
