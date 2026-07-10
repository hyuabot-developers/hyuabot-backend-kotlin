package app.hyuabot.backend.shuttle

import app.hyuabot.backend.codegen.types.ShuttleLimitInput
import app.hyuabot.backend.database.entity.ShuttleHoliday
import app.hyuabot.backend.database.entity.ShuttlePeriod
import app.hyuabot.backend.database.entity.ShuttleStop
import app.hyuabot.backend.shuttle.controller.ShuttleDataFetcher
import app.hyuabot.backend.shuttle.controller.ShuttleTimetableDataLoader
import app.hyuabot.backend.shuttle.domain.ShuttleArrivalItem
import app.hyuabot.backend.shuttle.domain.ShuttleHolidayOccurrence
import app.hyuabot.backend.shuttle.domain.ShuttleTimetableKey
import app.hyuabot.backend.shuttle.domain.ShuttleTimetableResult
import app.hyuabot.backend.shuttle.domain.ShuttleTimetableViewItem
import app.hyuabot.backend.shuttle.service.ShuttleHolidayService
import app.hyuabot.backend.shuttle.service.ShuttlePeriodService
import app.hyuabot.backend.shuttle.service.ShuttleStopService
import app.hyuabot.backend.shuttle.service.ShuttleTimetableService
import app.hyuabot.backend.utility.ScalarRegistration
import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.test.EnableDgsTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@EnableDgsTest
@SpringJUnitConfig
@Import(ShuttleDataFetcher::class, ShuttleTimetableDataLoader::class, ScalarRegistration::class)
class ShuttleDataFetcherTest {
    @Autowired lateinit var dgsQueryExecutor: DgsQueryExecutor

    @MockitoBean lateinit var holidayService: ShuttleHolidayService

    @MockitoBean lateinit var periodService: ShuttlePeriodService

    @MockitoBean lateinit var stopService: ShuttleStopService

    @MockitoBean lateinit var timetableService: ShuttleTimetableService

    private val today = LocalDate.parse("2026-03-25")

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private fun createPeriod(
        seq: Int = 1,
        type: String = "semester",
        start: ZonedDateTime = ZonedDateTime.now().minusDays(30),
        end: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ) = ShuttlePeriod(
        seq = seq,
        type = type,
        start = start,
        end = end,
        periodType = null,
    )

    private fun createHoliday(
        seq: Int = 1,
        date: LocalDate = LocalDate.parse("2026-03-25"),
        type: String = "weekends",
        calendarType: String = "solar",
    ) = ShuttleHoliday(
        seq = seq,
        date = date,
        type = type,
        calendarType = calendarType,
    )

    private fun createStop(
        name: String = "dormitory_o",
        latitude: Double = 37.0,
        longitude: Double = 125.0,
    ) = ShuttleStop(
        name = name,
        latitude = latitude,
        longitude = longitude,
        route = mutableListOf(),
        routeToStart = mutableListOf(),
        routeToEnd = mutableListOf(),
    )

    private fun createTimetableView(
        seq: Int = 1,
        routeName: String = "DHDD",
        routeTag: String = "DH",
        period: String = "semester",
        weekday: Boolean = true,
        time: LocalTime = LocalTime.parse("10:00"),
        group: String = "STATION",
        stops: List<ShuttleArrivalItem> =
            listOf(
                ShuttleArrivalItem(stop = "dormitory_o", LocalTime.parse("10:00")),
                ShuttleArrivalItem(stop = "shuttlecock_o", LocalTime.parse("10:05")),
                ShuttleArrivalItem(stop = "station", LocalTime.parse("10:15")),
            ),
    ) = ShuttleTimetableViewItem(
        seq = seq,
        routeName = routeName,
        routeTag = routeTag,
        period = period,
        weekday = weekday,
        time = time,
        group = group,
        stops = stops,
    )

    private fun createTimetableResult(items: List<ShuttleTimetableViewItem> = listOf(createTimetableView())) =
        ShuttleTimetableResult(
            order = items,
            destination = items.groupBy { it.group }.filterKeys { it.isNotEmpty() },
        )

    @Test
    @DisplayName("셔틀버스 GraphQL Argument 검증")
    fun testShuttleGraphQLArgumentValidation() {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dates = listOf(dateFormatter.format(today), null)
        val weekdays = listOf(listOf(true), emptyList(), null)
        val periods = listOf(listOf("semester"), emptyList(), null)
        var result: Map<String, Any>?
        whenever(holidayService.findShuttleHoliday(any())).thenReturn(createHoliday())
        whenever(periodService.findShuttlePeriod(any())).thenReturn(createPeriod())
        dates.forEach { date ->
            weekdays.forEach { weekday ->
                periods.forEach { period ->
                    if (date != null && (!period.isNullOrEmpty() || !weekday.isNullOrEmpty())) {
                        val periodString =
                            period?.joinToString(
                                prefix = "[",
                                postfix = "]",
                            ) {
                                "\"$it\""
                            }
                        assert(
                            dgsQueryExecutor
                                .execute(
                                    """
                                    {
                                        shuttle(
                                            input: {
                                                date: "$date",
                                                weekdays: $weekday,
                                                periods: $periodString,
                                            }
                                        ) {
                                            period {
                                                type, start, end
                                            }
                                            holiday {
                                                date, type, calendar                    
                                            }
                                        }
                                    }
                                    """.trimIndent(),
                                ).errors
                                .isNotEmpty(),
                        )
                    } else if (date != null) {
                        result =
                            dgsQueryExecutor.executeAndExtractJsonPath<Map<String, Any>>(
                                """
                                {
                                    shuttle(
                                        input: {
                                            date: "$date",
                                            weekdays: $weekday,
                                            periods: $period
                                        }
                                    ) {
                                        period {
                                            type, start, end
                                        }
                                        holiday {
                                            date, type, calendar                    
                                        }
                                    }
                                }
                                """.trimIndent(),
                                "data.shuttle",
                            )
                        assertNotNull(result)
                        val period = result!!["period"] as Map<*, *>
                        assertEquals("semester", period["type"])
                        assertNotNull(period["start"])
                        assertNotNull(period["end"])
                        val holiday = result!!["holiday"] as Map<*, *>
                        assertEquals(date, holiday["date"])
                        assertEquals("weekends", holiday["type"])
                        assertEquals("solar", holiday["calendar"])
                    } else {
                        val periodString =
                            period?.joinToString(
                                prefix = "[",
                                postfix = "]",
                            ) {
                                "\"$it\""
                            }
                        result =
                            dgsQueryExecutor.executeAndExtractJsonPath<Map<String, Any>>(
                                """
                                {
                                    shuttle(
                                        input: {
                                            weekdays: $weekday,
                                            periods: $periodString
                                        }
                                    ) {
                                        period {
                                            type, start, end
                                        }
                                        holiday {
                                            date, type, calendar                    
                                        }
                                    }
                                }
                                """.trimIndent(),
                                "data.shuttle",
                            )
                        assertNotNull(result)
                        assertNull(result["period"])
                        assertNull(result["holiday"])
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("셔틀버스 Date 기준으로 period, holiday 검색")
    fun testShuttleFindPeriodAndHoliday() {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dates =
            listOf(
                dateFormatter.format(today),
                dateFormatter.format(today.plusDays(1)),
                dateFormatter.format(today.plusDays(2)),
                dateFormatter.format(today.plusDays(3)),
                dateFormatter.format(today.plusDays(4)),
            )
        var result: Map<String, Any>?
        whenever(holidayService.findShuttleHoliday(today)).thenReturn(createHoliday())
        whenever(holidayService.findShuttleHoliday(today.plusDays(1))).thenReturn(null)
        whenever(holidayService.findShuttleHoliday(today.plusDays(2))).thenReturn(
            createHoliday(
                type = "halt",
                date = today.plusDays(2),
            ),
        )
        whenever(holidayService.findShuttleHoliday(today.plusDays(3))).thenReturn(null)
        whenever(holidayService.findShuttleHoliday(today.plusDays(4))).thenReturn(null)
        whenever(periodService.findShuttlePeriod(today)).thenReturn(null)
        whenever(periodService.findShuttlePeriod(today.plusDays(1))).thenReturn(createPeriod())
        whenever(periodService.findShuttlePeriod(today.plusDays(2))).thenReturn(createPeriod())
        whenever(periodService.findShuttlePeriod(today.plusDays(3))).thenReturn(createPeriod())
        whenever(periodService.findShuttlePeriod(today.plusDays(4))).thenReturn(createPeriod())
        dates.forEach { date ->
            result =
                dgsQueryExecutor.executeAndExtractJsonPath<Map<String, Any>>(
                    """
                    {
                        shuttle(
                            input: {
                                date: "$date",
                            }
                        ) {
                            period {
                                type, start, end
                            }
                            holiday {
                                date, type, calendar                    
                            }
                        }
                    }
                    """.trimIndent(),
                    "data.shuttle",
                )
            assertNotNull(result)
            if (date == dateFormatter.format(today)) {
                assertNull(result["period"])
            } else {
                val period = result["period"] as Map<*, *>
                assertEquals("semester", period["type"])
                assertNotNull(period["start"])
                assertNotNull(period["end"])
            }
            when (date) {
                dateFormatter.format(today) -> {
                    val holiday = result["holiday"] as Map<*, *>
                    assertEquals(date, holiday["date"])
                    assertEquals("weekends", holiday["type"])
                    assertEquals("solar", holiday["calendar"])
                }

                dateFormatter.format(today.plusDays(2)) -> {
                    val holiday = result["holiday"] as Map<*, *>
                    assertEquals(date, holiday["date"])
                    assertEquals("halt", holiday["type"])
                    assertEquals("solar", holiday["calendar"])
                }

                else -> {
                    assertNull(result["holiday"])
                }
            }
        }
    }

    @Test
    @DisplayName("셔틀버스 운행 변경 알림 후보 조회")
    fun testShuttleServiceNotices() {
        val start = today
        val end = today.plusDays(7)
        whenever(periodService.findShuttlePeriodsStartingBetween(start, end)).thenReturn(
            listOf(
                createPeriod(
                    seq = 2,
                    type = "vacation",
                    start = ZonedDateTime.parse("2026-03-25T15:00:00.000Z"),
                    end = ZonedDateTime.parse("2026-04-30T23:59:59.999+09:00"),
                ),
            ),
        )
        whenever(holidayService.findShuttleHolidayOccurrences(start, end)).thenReturn(
            listOf(
                ShuttleHolidayOccurrence(
                    date = today.plusDays(1),
                    holiday =
                        createHoliday(
                            seq = 3,
                            date = LocalDate.parse("2026-03-26"),
                            type = "halt",
                        ),
                ),
            ),
        )

        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    shuttle(input: { date: "${dateFormatter.format(today)}" }) {
                        serviceNotices(start: "${dateFormatter.format(start)}", end: "${dateFormatter.format(end)}") {
                            id
                            kind
                            date
                            period { seq, type, start, end }
                            holiday { seq, date, type, calendar }
                        }
                    }
                }
                """.trimIndent(),
                "data.shuttle.serviceNotices",
            )

        assertEquals(2, result.size)
        assertEquals("holiday:3:2026-03-26", result[0]["id"])
        assertEquals("holiday", result[0]["kind"])
        assertEquals("2026-03-26", result[0]["date"])
        val holiday = result[0]["holiday"] as Map<*, *>
        assertEquals(3, holiday["seq"])
        assertEquals("halt", holiday["type"])
        assertEquals("solar", holiday["calendar"])

        assertEquals("period:2:2026-03-26", result[1]["id"])
        assertEquals("period", result[1]["kind"])
        assertEquals("2026-03-26", result[1]["date"])
        val period = result[1]["period"] as Map<*, *>
        assertEquals(2, period["seq"])
        assertEquals("vacation", period["type"])
    }

    @Test
    @DisplayName("셔틀버스 운행 변경 알림 후보 조회 - 잘못된 범위")
    fun testShuttleServiceNoticesInvalidRange() {
        val result =
            dgsQueryExecutor.execute(
                """
                {
                    shuttle(input: { date: "${dateFormatter.format(today)}" }) {
                        serviceNotices(start: "2026-04-01", end: "2026-03-01") {
                            id
                        }
                    }
                }
                """.trimIndent(),
            )

        assert(result.errors.isNotEmpty())
    }

    @Test
    @DisplayName("셔틀버스 정류장 목록 조회 - 전체 조회")
    fun testAllShuttleStop() {
        whenever(stopService.getAllStops()).thenReturn(
            listOf(
                createStop(name = "Stop1"),
                createStop(name = "Stop2"),
            ),
        )
        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    shuttle(
                        input: {
                            date: "${dateFormatter.format(today)}"
                        }
                    ){
                        stops {
                            name
                            latitude
                            longitude
                        }
                    }
                }
                """.trimIndent(),
                "data.shuttle.stops",
            )
        assertEquals(2, result!!.size)
        assertEquals("Stop1", result[0]["name"])
        assertEquals("Stop2", result[1]["name"])
    }

    @Test
    @DisplayName("셔틀버스 정류장 목록 조회 - ID 목록으로 필터링")
    fun testShuttleStopWithIDList() {
        whenever(
            stopService.getStopsByNames(
                listOf(
                    "Stop1",
                    "Stop2",
                ),
            ),
        ).thenReturn(
            listOf(
                createStop(name = "Stop1"),
                createStop(name = "Stop2"),
            ),
        )
        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    shuttle(
                        input: {
                            stops: [
                                { name: "Stop1", limit: { order: null, destination: null } },
                                { name: "Stop2", limit: { order: 1, destination: 1 } }
                            ],
                            date: "${dateFormatter.format(today)}"
                        }
                    ){
                        stops {
                            name
                            latitude
                            longitude
                        }
                    }
                }
                """.trimIndent(),
                "data.shuttle.stops",
            )
        assertEquals(2, result!!.size)
        assertEquals("Stop1", result[0]["name"])
        assertEquals("Stop2", result[1]["name"])
    }

    @Test
    @DisplayName("셔틀버스 시간표 조회 - period / weekdays 지정")
    fun testShuttleTimetableWithPeriodAndWeekdays() {
        whenever(
            stopService.getStopsByNames(
                listOf(
                    "dormitory_o",
                    "shuttlecock_o",
                ),
            ),
        ).thenReturn(
            listOf(
                createStop(name = "dormitory_o"),
                createStop(name = "shuttlecock_o"),
            ),
        )
        whenever(timetableService.getShuttleTimetableBatch(any())).thenReturn(
            mapOf(
                ShuttleTimetableKey(
                    stop = "dormitory_o",
                    periods = setOf("semester"),
                    weekdays = setOf(true),
                    after = null,
                    limit = ShuttleLimitInput(order = null, destination = null),
                    destinations = setOf("TERMINAL"),
                    routes = setOf("CDD"),
                    tags = setOf("C"),
                ) to createTimetableResult(),
                ShuttleTimetableKey(
                    stop = "shuttlecock_o",
                    periods = setOf("semester"),
                    weekdays = setOf(true),
                    after = null,
                    limit = ShuttleLimitInput(order = 1, destination = 1),
                    destinations = null,
                    routes = null,
                    tags = null,
                ) to createTimetableResult(),
            ),
        )
        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    shuttle(
                        input: {
                            stops: [
                                { name: "dormitory_o", limit: { order: null, destination: null }, destinations: ["TERMINAL"], routes: ["CDD"], tags: ["C"] },
                                { name: "shuttlecock_o", limit: { order: 1, destination: 1 } }
                            ],
                            periods: ["semester"],
                            weekdays: [true]
                        }
                    ){
                        stops {
                            name
                            timetable {
                                order {
                                    seq
                                    route { name, tag }
                                    period
                                    weekday
                                    time
                                    stops { stop, time }
                                }
                                destination {
                                    destination                                
                                    entries {
                                        seq
                                        route { name, tag }
                                        period
                                        weekday
                                        time
                                        stops { stop, time }
                                    }
                                }
                            }
                        }
                    }
                }
                """.trimIndent(),
                "data.shuttle.stops",
            )
        assertEquals(2, result!!.size)
        val timetable = result[0]["timetable"] as Map<*, *>
        assertNotNull(timetable)
        val timetableByOrder = timetable["order"] as List<*>
        assertEquals(1, timetableByOrder.size)
        timetableByOrder.forEach {
            val item = it as Map<*, *>
            assertEquals(1, item["seq"])
            val route = item["route"] as Map<*, *>
            assertEquals("DHDD", route["name"])
            assertEquals("DH", route["tag"])
            assertEquals("semester", item["period"])
            assertEquals(true, item["weekday"])
            assertEquals("10:00:00", item["time"])
            val stops = item["stops"] as List<*>
            assertEquals(3, stops.size)
            val stop1 = stops[0] as Map<*, *>
            assertEquals("dormitory_o", stop1["stop"])
            assertEquals("10:00:00", stop1["time"])
            val stop2 = stops[1] as Map<*, *>
            assertEquals("shuttlecock_o", stop2["stop"])
            assertEquals("10:05:00", stop2["time"])
            val stop3 = stops[2] as Map<*, *>
            assertEquals("station", stop3["stop"])
            assertEquals("10:15:00", stop3["time"])
        }
    }

    @Test
    @DisplayName("셔틀버스 시간표 조회 - period / weekdays 지정 (운행 중지의 날)")
    fun testShuttleTimetableWithPeriodAndWeekdaysOnHaltDay() {
        whenever(periodService.findShuttlePeriod(any())).thenReturn(createPeriod())
        whenever(holidayService.findShuttleHoliday(any())).thenReturn(
            createHoliday(
                type = "halt",
                date = today,
            ),
        )
        whenever(stopService.getAllStops()).thenReturn(listOf(createStop()))
        whenever(timetableService.getShuttleTimetableBatch(any())).thenReturn(
            mapOf(
                ShuttleTimetableKey(
                    stop = "dormitory_o",
                    periods = setOf("semester"),
                    weekdays = setOf(true),
                    after = null,
                    limit = ShuttleLimitInput(order = null, destination = null),
                    destinations = null,
                    routes = null,
                    tags = null,
                ) to createTimetableResult(),
            ),
        )
        val result =
            dgsQueryExecutor.executeAndExtractJsonPath<List<Map<String, Any>>>(
                """
                {
                    shuttle(
                        input: {
                            date: "${dateFormatter.format(today)}",
                        }
                    ){
                        stops {
                            name
                            timetable {
                                order {
                                    seq
                                    route { name, tag }
                                    period
                                    weekday
                                    time
                                    stops { stop, time }
                                }
                            }
                        }
                    }
                }
                """.trimIndent(),
                "data.shuttle.stops",
            )
        assertEquals(1, result!!.size)
        val timetable = result[0]["timetable"] as Map<*, *>
        assertEquals(0, (timetable["order"] as List<*>).size)
    }
}
