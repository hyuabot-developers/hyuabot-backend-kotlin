package app.hyuabot.backend.shuttle

import app.hyuabot.backend.codegen.types.ShuttleLimitInput
import app.hyuabot.backend.database.entity.ShuttleTimetableView
import app.hyuabot.backend.database.repository.ShuttleTimetableViewRepository
import app.hyuabot.backend.shuttle.domain.ShuttleTimetableKey
import app.hyuabot.backend.shuttle.service.ShuttleTimetableService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.LocalTime
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class ShuttleTimetableServiceTest {
    @Mock
    private lateinit var shuttleTimetableViewRepository: ShuttleTimetableViewRepository

    @InjectMocks
    private lateinit var shuttleTimetableService: ShuttleTimetableService

    @Test
    @DisplayName("셔틀버스 시간표 (뷰) 목록 조회")
    fun getShuttleTimetable() {
        whenever(shuttleTimetableViewRepository.findAll())
            .thenReturn(
                listOf(
                    ShuttleTimetableView(
                        seq = 1,
                        periodType = "semester",
                        weekday = true,
                        routeName = "A",
                        routeTag = "A",
                        stopName = "Stop1",
                        departureTime = LocalTime.parse("09:00:00"),
                        destinationGroup = "Group1",
                    ),
                    ShuttleTimetableView(
                        seq = 2,
                        periodType = "semester",
                        weekday = true,
                        routeName = "B",
                        routeTag = "B",
                        stopName = "Stop2",
                        departureTime = LocalTime.parse("10:00:00"),
                        destinationGroup = "Group2",
                    ),
                ),
            )
        val result = shuttleTimetableService.getShuttleTimetable()
        assertEquals(2, result.size)
        assertEquals("A", result[0].routeName)
        assertEquals("Stop1", result[0].stopName)
        assertEquals(LocalTime.parse("09:00:00"), result[0].departureTime)
        assertEquals("B", result[1].routeName)
        assertEquals("Stop2", result[1].stopName)
        assertEquals(LocalTime.parse("10:00:00"), result[1].departureTime)
    }

    @Test
    @DisplayName("셔틀버스 시간표 (뷰) 배치 조회 - 키가 빈 배열인 경우")
    fun getShuttleTimetableBatchWithEmptyKeys() {
        val result = shuttleTimetableService.getShuttleTimetableBatch(emptySet())
        assertEquals(0, result.size)
    }

    @Test
    @DisplayName("셔틀버스 시간표 (뷰) 배치 조회 - 키에 해당하는 periods가 빈 경우")
    fun getShuttleTimetableBatchWithEmptyPeriods() {
        val key =
            ShuttleTimetableKey(
                stop = "dormitory_o",
                periods = emptySet(),
                weekdays = setOf(true),
                after = null,
                limit = ShuttleLimitInput(order = null, destination = null),
                destinations = null,
                routes = null,
                tags = null,
            )

        val result = shuttleTimetableService.getShuttleTimetableBatch(setOf(key))
        assertEquals(1, result.size)
        assertEquals(0, result[key]!!.order.size)
        assertEquals(0, result[key]!!.destination.size)
    }

    @Test
    @DisplayName("셔틀버스 시간표 (뷰) 배치 조회 - 키에 해당하는 weekdays 빈 경우")
    fun getShuttleTimetableBatchWithEmptyWeekdays() {
        val key =
            ShuttleTimetableKey(
                stop = "dormitory_o",
                periods = setOf("semester"),
                weekdays = emptySet(),
                after = null,
                limit = ShuttleLimitInput(order = null, destination = null),
                destinations = null,
                routes = null,
                tags = null,
            )

        val result = shuttleTimetableService.getShuttleTimetableBatch(setOf(key))
        assertEquals(1, result.size)
        assertEquals(0, result[key]!!.order.size)
        assertEquals(0, result[key]!!.destination.size)
    }

    @Test
    @DisplayName("셔틀버스 시간표 (뷰) 배치 조회")
    fun getShuttleTimetableBatch() {
        val key =
            ShuttleTimetableKey(
                stop = "dormitory_o",
                periods = setOf("semester"),
                weekdays = setOf(true),
                after = null,
                limit = ShuttleLimitInput(order = null, destination = null),
                destinations = null,
                routes = null,
                tags = null,
            )
        whenever(
            shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsIn(
                periods = listOf("semester"),
                weekdays = listOf(true),
                stops = listOf(key.stop),
            ),
        ).thenReturn(
            listOf(
                ShuttleTimetableView(
                    seq = 1,
                    periodType = "semester",
                    weekday = true,
                    routeName = "A",
                    routeTag = "A",
                    stopName = "dormitory_o",
                    departureTime = LocalTime.parse("09:00:00"),
                    destinationGroup = "Group1",
                ),
                ShuttleTimetableView(
                    seq = 2,
                    periodType = "semester",
                    weekday = true,
                    routeName = "B",
                    routeTag = "B",
                    stopName = "dormitory_o",
                    departureTime = LocalTime.parse("10:00:00"),
                    destinationGroup = "Group2",
                ),
            ),
        )
        val result = shuttleTimetableService.getShuttleTimetableBatch(setOf(key))
        assertEquals(1, result.size)
        val timetable = result[key]!!
        assertEquals(2, timetable.order.size)
        assertEquals(LocalTime.parse("09:00"), timetable.order[0].time)
        assertEquals(LocalTime.parse("10:00"), timetable.order[1].time)
    }

    @Test
    @DisplayName("셔틀버스 시간표 (뷰) 배치 조회 - destination 그룹핑")
    fun getShuttleTimetableBatchWithDestinationGrouping() {
        val key =
            ShuttleTimetableKey(
                stop = "dormitory_o",
                periods = setOf("semester"),
                weekdays = setOf(true),
                after = null,
                limit = ShuttleLimitInput(order = null, destination = null),
                destinations = null,
                routes = null,
                tags = null,
            )
        whenever(
            shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsIn(
                periods = listOf("semester"),
                weekdays = listOf(true),
                stops = listOf(key.stop),
            ),
        ).thenReturn(
            listOf(
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "dormitory_o",
                    departureTime = LocalTime.parse("09:00:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "shuttlecock_o",
                    departureTime = LocalTime.parse("09:05:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "station",
                    departureTime = LocalTime.parse("09:15:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "dormitory_o",
                    departureTime = LocalTime.parse("09:00:00"),
                    destinationGroup = "TERMINAL",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "shuttlecock_o",
                    departureTime = LocalTime.parse("09:05:00"),
                    destinationGroup = "TERMINAL",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "station",
                    departureTime = LocalTime.parse("09:20:00"),
                    destinationGroup = "TERMINAL",
                    periodType = "semester",
                    weekday = true,
                ),
            ),
        )
        val result = shuttleTimetableService.getShuttleTimetableBatch(setOf(key))
        val timetable = result[key]!!
        assertEquals(1, timetable.order.size)
        assertEquals(2, timetable.destination.size)
        assertEquals("STATION", timetable.destination.keys.first())
        assertEquals("TERMINAL", timetable.destination.keys.last())
        assertEquals(1, timetable.destination["STATION"]!!.size)
        assertEquals(1, timetable.destination["TERMINAL"]!!.size)
    }

    @Test
    @DisplayName("셔틀버스 시간표 (뷰) 배치 조회 - after 필터링")
    fun getShuttleTimetableBatchWithAfterFiltering() {
        val key =
            ShuttleTimetableKey(
                stop = "dormitory_o",
                periods = setOf("semester"),
                weekdays = setOf(true),
                after = LocalTime.parse("09:10"),
                limit = ShuttleLimitInput(order = null, destination = null),
                destinations = null,
                routes = null,
                tags = null,
            )
        whenever(
            shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsIn(
                periods = listOf("semester"),
                weekdays = listOf(true),
                stops = listOf(key.stop),
            ),
        ).thenReturn(
            listOf(
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "dormitory_o",
                    departureTime = LocalTime.parse("09:00:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "shuttlecock_o",
                    departureTime = LocalTime.parse("09:05:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "station",
                    departureTime = LocalTime.parse("09:15:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 2,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "dormitory_o",
                    departureTime = LocalTime.parse("09:20:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 2,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "shuttlecock_o",
                    departureTime = LocalTime.parse("09:25:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 2,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "station",
                    departureTime = LocalTime.parse("09:35:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
            ),
        )
        whenever(
            shuttleTimetableViewRepository
                .findBySeqIn(
                    listOf(1, 2),
                ),
        ).thenReturn(
            listOf(
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "dormitory_o",
                    departureTime = LocalTime.parse("09:00:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "shuttlecock_o",
                    departureTime = LocalTime.parse("09:05:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "station",
                    departureTime = LocalTime.parse("09:15:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 2,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "dormitory_o",
                    departureTime = LocalTime.parse("09:20:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 2,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "shuttlecock_o",
                    departureTime = LocalTime.parse("09:25:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 2,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "station",
                    departureTime = LocalTime.parse("09:35:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
            ),
        )
        val result = shuttleTimetableService.getShuttleTimetableBatch(setOf(key))
        val timetable = result[key]!!
        assertEquals(1, timetable.order.size)
        assertEquals(LocalTime.parse("09:20"), timetable.order[0].time)
        assertEquals(3, timetable.order[0].stops.size)
    }

    @Test
    @DisplayName("셔틀버스 시간표 (뷰) 배치 조회 - limit 적용")
    fun getShuttleTimetableBatchWithLimit() {
        val key =
            ShuttleTimetableKey(
                stop = "dormitory_o",
                periods = setOf("semester"),
                weekdays = setOf(true),
                after = null,
                limit = ShuttleLimitInput(order = 1, destination = null),
                destinations = null,
                routes = null,
                tags = null,
            )
        whenever(
            shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsIn(
                periods = listOf("semester"),
                weekdays = listOf(true),
                stops = listOf(key.stop),
            ),
        ).thenReturn(
            listOf(
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "dormitory_o",
                    departureTime = LocalTime.parse("09:00:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "shuttlecock_o",
                    departureTime = LocalTime.parse("09:05:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "station",
                    departureTime = LocalTime.parse("09:15:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 2,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "dormitory_o",
                    departureTime = LocalTime.parse("09:20:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 2,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "shuttlecock_o",
                    departureTime = LocalTime.parse("09:25:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 2,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "station",
                    departureTime = LocalTime.parse("09:35:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
            ),
        )
        whenever(
            shuttleTimetableViewRepository
                .findBySeqIn(
                    listOf(1, 2),
                ),
        ).thenReturn(
            listOf(
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "dormitory_o",
                    departureTime = LocalTime.parse("09:00:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "shuttlecock_o",
                    departureTime = LocalTime.parse("09:05:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "station",
                    departureTime = LocalTime.parse("09:15:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 2,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "dormitory_o",
                    departureTime = LocalTime.parse("09:20:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 2,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "shuttlecock_o",
                    departureTime = LocalTime.parse("09:25:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 2,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "station",
                    departureTime = LocalTime.parse("09:35:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
            ),
        )
        val result = shuttleTimetableService.getShuttleTimetableBatch(setOf(key))
        val timetable = result[key]!!
        assertEquals(1, timetable.order.size)
        assertEquals(LocalTime.parse("09:00"), timetable.order[0].time)
        assertEquals(3, timetable.order[0].stops.size)
    }

    @Test
    @DisplayName("셔틀 시간표 배치 조회 - 결과 없음")
    fun testGetShuttleTimetableBatchEmpty() {
        val key =
            ShuttleTimetableKey(
                stop = "dormitory_o",
                periods = setOf("semester"),
                weekdays = setOf(true),
                after = null,
                limit = ShuttleLimitInput(order = null, destination = null),
                destinations = null,
                routes = null,
                tags = null,
            )
        whenever(
            shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsIn(
                periods = listOf("semester"),
                weekdays = listOf(true),
                stops = listOf("dormitory_o"),
            ),
        ).thenReturn(emptyList())

        val result = shuttleTimetableService.getShuttleTimetableBatch(setOf(key))

        val timetable = result[key]!!
        assertEquals(0, timetable.order.size)
        assertEquals(0, timetable.destination.size)
    }

    @Test
    @DisplayName("셔틀 시간표 배치 조회 - 여러 정류장")
    fun testGetShuttleTimetableBatchMultipleStops() {
        val key1 =
            ShuttleTimetableKey(
                stop = "dormitory_o",
                periods = setOf("semester"),
                weekdays = setOf(true),
                after = null,
                limit = ShuttleLimitInput(order = null, destination = null),
                destinations = null,
                routes = null,
                tags = null,
            )
        val key2 =
            ShuttleTimetableKey(
                stop = "shuttlecock_o",
                periods = setOf("semester"),
                weekdays = setOf(true),
                after = null,
                limit = ShuttleLimitInput(order = 1, destination = 1),
                destinations = null,
                routes = null,
                tags = null,
            )
        whenever(
            shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsIn(
                periods = listOf("semester"),
                weekdays = listOf(true),
                stops = listOf("dormitory_o", "shuttlecock_o"),
            ),
        ).thenReturn(
            listOf(
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "dormitory_o",
                    departureTime = LocalTime.parse("09:00:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "shuttlecock_o",
                    departureTime = LocalTime.parse("09:05:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "station",
                    departureTime = LocalTime.parse("09:15:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
            ),
        )
        whenever(
            shuttleTimetableViewRepository
                .findBySeqIn(
                    listOf(1),
                ),
        ).thenReturn(
            listOf(
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "dormitory_o",
                    departureTime = LocalTime.parse("09:00:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "shuttlecock_o",
                    departureTime = LocalTime.parse("09:05:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "station",
                    departureTime = LocalTime.parse("09:15:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
            ),
        )

        val result = shuttleTimetableService.getShuttleTimetableBatch(setOf(key1, key2))

        assertEquals(2, result.size)
        assertEquals(1, result[key1]?.order?.size)
        assertEquals(LocalTime.parse("09:00"), result[key1]?.order?.get(0)?.time)
        assertEquals(1, result[key2]?.order?.size)
        assertEquals(LocalTime.parse("09:05"), result[key2]?.order?.get(0)?.time)
        assertEquals(
            3,
            result[key1]
                ?.order
                ?.get(0)
                ?.stops
                ?.size,
        )
        assertEquals(
            3,
            result[key2]
                ?.order
                ?.get(0)
                ?.stops
                ?.size,
        )
    }

    @Test
    @DisplayName("셔틀 시간표 배치 조회 - routes 필터링 (매칭 안됨)")
    fun testGetShuttleTimetableBatchRoutesNotMatch() {
        whenever(
            shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteNameIsIn(
                periods = listOf("semester"),
                stops = listOf("dormitory_o"),
                weekdays = listOf(true),
                routes = listOf("DH"),
            ),
        ).thenReturn(
            listOf(
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "DH",
                    routeName = "DHDD",
                    stopName = "dormitory_o",
                    departureTime = LocalTime.parse("09:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
            ),
        )

        val result =
            shuttleTimetableService.getShuttleTimetableBatch(
                setOf(
                    ShuttleTimetableKey(
                        stop = "dormitory_o",
                        periods = setOf("semester"),
                        weekdays = setOf(true),
                        after = null,
                        limit = ShuttleLimitInput(order = null, destination = null),
                        destinations = null,
                        routes = setOf("DH"),
                        tags = null,
                    ),
                ),
            )

        assertEquals(
            0,
            result.values
                .first()
                .order.size,
        )
    }

    @Test
    @DisplayName("셔틀 시간표 배치 조회 - tags 필터링 (매칭 안됨)")
    fun testGetShuttleTimetableBatchTagsNotMatch() {
        whenever(
            shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteTagIsIn(
                periods = listOf("semester"),
                stops = listOf("dormitory_o"),
                weekdays = listOf(true),
                tags = listOf("DH"),
            ),
        ).thenReturn(
            listOf(
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "dormitory_o",
                    departureTime = LocalTime.parse("09:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
            ),
        )

        val result =
            shuttleTimetableService.getShuttleTimetableBatch(
                setOf(
                    ShuttleTimetableKey(
                        stop = "dormitory_o",
                        periods = setOf("semester"),
                        weekdays = setOf(true),
                        after = null,
                        limit = ShuttleLimitInput(order = null, destination = null),
                        destinations = null,
                        routes = null,
                        tags = setOf("DH"),
                    ),
                ),
            )

        assertEquals(
            0,
            result.values
                .first()
                .order.size,
        )
    }

    @Test
    @DisplayName("셔틀 시간표 배치 조회 - destinations 필터링 (매칭 안됨)")
    fun testGetShuttleTimetableBatchDestinationsNotMatch() {
        whenever(
            shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndDestinationGroupIsIn(
                periods = listOf("semester"),
                stops = listOf("dormitory_o"),
                weekdays = listOf(true),
                groups = listOf("TERMINAL"),
            ),
        ).thenReturn(
            listOf(
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "DH",
                    routeName = "DH",
                    stopName = "dormitory_o",
                    departureTime = LocalTime.parse("09:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
            ),
        )

        val result =
            shuttleTimetableService.getShuttleTimetableBatch(
                setOf(
                    ShuttleTimetableKey(
                        stop = "dormitory_o",
                        periods = setOf("semester"),
                        weekdays = setOf(true),
                        after = null,
                        limit = ShuttleLimitInput(order = null, destination = null),
                        destinations = setOf("TERMINAL"),
                        routes = null,
                        tags = null,
                    ),
                ),
            )

        assertEquals(
            0,
            result.values
                .first()
                .order.size,
        )
    }

    @Test
    @DisplayName("셔틀 시간표 배치 조회 - 노선명/노선 태그/목적지 필터링")
    fun testGetShuttleTimetableBatchWithRouteAndDestinationFiltering() {
        val testRoutes = listOf(listOf("CDD"), null)
        val testTags = listOf(listOf("C"), null)
        val testDestinations = listOf(listOf("STATION"), null)
        val repositoryReturnValue =
            listOf(
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "dormitory_o",
                    departureTime = LocalTime.parse("09:00:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "shuttlecock_o",
                    departureTime = LocalTime.parse("09:05:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
                ShuttleTimetableView(
                    seq = 1,
                    routeTag = "C",
                    routeName = "CDD",
                    stopName = "station",
                    departureTime = LocalTime.parse("09:15:00"),
                    destinationGroup = "STATION",
                    periodType = "semester",
                    weekday = true,
                ),
            )
        whenever(
            shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsIn(
                periods = listOf("semester"),
                stops = listOf("dormitory_o"),
                weekdays = listOf(true),
            ),
        ).thenReturn(repositoryReturnValue)
        whenever(
            shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteNameIsIn(
                periods = listOf("semester"),
                stops = listOf("dormitory_o"),
                weekdays = listOf(true),
                routes = listOf("CDD"),
            ),
        ).thenReturn(repositoryReturnValue)
        whenever(
            shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteTagIsIn(
                periods = listOf("semester"),
                stops = listOf("dormitory_o"),
                weekdays = listOf(true),
                tags = listOf("C"),
            ),
        ).thenReturn(repositoryReturnValue)
        whenever(
            shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndDestinationGroupIsIn(
                periods = listOf("semester"),
                stops = listOf("dormitory_o"),
                weekdays = listOf(true),
                groups = listOf("STATION"),
            ),
        ).thenReturn(repositoryReturnValue)
        whenever(
            shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteNameIsInAndRouteTagIsIn(
                periods = listOf("semester"),
                stops = listOf("dormitory_o"),
                weekdays = listOf(true),
                routes = listOf("CDD"),
                tags = listOf("C"),
            ),
        ).thenReturn(repositoryReturnValue)
        whenever(
            shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteNameIsInAndDestinationGroupIsIn(
                periods = listOf("semester"),
                stops = listOf("dormitory_o"),
                weekdays = listOf(true),
                routes = listOf("CDD"),
                groups = listOf("STATION"),
            ),
        ).thenReturn(repositoryReturnValue)
        whenever(
            shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteTagIsInAndDestinationGroupIsIn(
                periods = listOf("semester"),
                stops = listOf("dormitory_o"),
                weekdays = listOf(true),
                tags = listOf("C"),
                groups = listOf("STATION"),
            ),
        ).thenReturn(repositoryReturnValue)
        whenever(
            shuttleTimetableViewRepository
                .findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteNameIsInAndRouteTagIsInAndDestinationGroupIsIn(
                    periods = listOf("semester"),
                    stops = listOf("dormitory_o"),
                    weekdays = listOf(true),
                    routes = listOf("CDD"),
                    tags = listOf("C"),
                    groups = listOf("STATION"),
                ),
        ).thenReturn(repositoryReturnValue)
        whenever(
            shuttleTimetableViewRepository
                .findBySeqIn(
                    listOf(1),
                ),
        ).thenReturn(repositoryReturnValue)

        testRoutes.forEach { testRoute ->
            testTags.forEach { testTag ->
                testDestinations.forEach { testDestination ->
                    val result =
                        shuttleTimetableService.getShuttleTimetableBatch(
                            setOf(
                                ShuttleTimetableKey(
                                    stop = "dormitory_o",
                                    periods = setOf("semester"),
                                    weekdays = setOf(true),
                                    after = null,
                                    limit = ShuttleLimitInput(order = null, destination = null),
                                    destinations = testDestination?.toSet(),
                                    routes = testRoute?.toSet(),
                                    tags = testTag?.toSet(),
                                ),
                                ShuttleTimetableKey(
                                    stop = "dormitory_o",
                                    periods = setOf("semester"),
                                    weekdays = setOf(true),
                                    after = null,
                                    limit = ShuttleLimitInput(order = null, destination = null),
                                    destinations = testDestination?.toSet(),
                                    routes = testRoute?.toSet(),
                                    tags = testTag?.toSet(),
                                ),
                            ),
                        )
                    assertEquals(1, result.size)
                    val timetable = result.values.first()
                    assertEquals(1, timetable.order.size)
                    assertEquals(LocalTime.parse("09:00"), timetable.order[0].stops[0].time)
                    assertEquals(LocalTime.parse("09:05"), timetable.order[0].stops[1].time)
                    assertEquals(LocalTime.parse("09:15"), timetable.order[0].stops[2].time)
                }
            }
        }
    }

    @Test
    @DisplayName("셔틀 시간표 배치 조회 - 혼합 필터 조합 (일부 키는 필터 있음, 일부 키는 필터 없음)")
    fun testGetShuttleTimetableBatchMixedFilterCombinations() {
        // key1 requests only ROUTE_A; key2 has no route filter (should see all routes)
        val keyWithRouteFilter =
            ShuttleTimetableKey(
                stop = "dormitory_o",
                periods = setOf("semester"),
                weekdays = setOf(true),
                after = null,
                limit = ShuttleLimitInput(order = null, destination = null),
                destinations = null,
                routes = setOf("ROUTE_A"),
                tags = null,
            )
        val keyWithoutFilter =
            ShuttleTimetableKey(
                stop = "dormitory_o",
                periods = setOf("semester"),
                weekdays = setOf(true),
                after = null,
                limit = ShuttleLimitInput(order = null, destination = null),
                destinations = null,
                routes = null,
                tags = null,
            )
        val routeARow =
            ShuttleTimetableView(
                seq = 1,
                routeTag = "A",
                routeName = "ROUTE_A",
                stopName = "dormitory_o",
                departureTime = LocalTime.parse("09:00:00"),
                destinationGroup = "STATION",
                periodType = "semester",
                weekday = true,
            )
        val routeBRow =
            ShuttleTimetableView(
                seq = 2,
                routeTag = "B",
                routeName = "ROUTE_B",
                stopName = "dormitory_o",
                departureTime = LocalTime.parse("10:00:00"),
                destinationGroup = "TERMINAL",
                periodType = "semester",
                weekday = true,
            )
        // The key with route filter gets only ROUTE_A rows from the DB
        whenever(
            shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsInAndRouteNameIsIn(
                periods = listOf("semester"),
                stops = listOf("dormitory_o"),
                weekdays = listOf(true),
                routes = listOf("ROUTE_A"),
            ),
        ).thenReturn(listOf(routeARow))
        // The key without filter gets all rows from the DB
        whenever(
            shuttleTimetableViewRepository.findByPeriodTypeIsInAndStopNameIsInAndWeekdayIsIn(
                periods = listOf("semester"),
                stops = listOf("dormitory_o"),
                weekdays = listOf(true),
            ),
        ).thenReturn(listOf(routeARow, routeBRow))

        val result =
            shuttleTimetableService.getShuttleTimetableBatch(setOf(keyWithRouteFilter, keyWithoutFilter))

        assertEquals(2, result.size)
        // key with route filter should see only ROUTE_A
        assertEquals(1, result[keyWithRouteFilter]!!.order.size)
        assertEquals("ROUTE_A", result[keyWithRouteFilter]!!.order[0].routeName)
        // key without filter should see both routes
        assertEquals(2, result[keyWithoutFilter]!!.order.size)
    }
}
