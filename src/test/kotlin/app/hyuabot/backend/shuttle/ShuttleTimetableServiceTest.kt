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
            )
        val key2 =
            ShuttleTimetableKey(
                stop = "shuttlecock_o",
                periods = setOf("semester"),
                weekdays = setOf(true),
                after = null,
                limit = ShuttleLimitInput(order = 1, destination = 1),
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
}
