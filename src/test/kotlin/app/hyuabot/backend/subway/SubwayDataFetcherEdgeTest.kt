package app.hyuabot.backend.subway

import app.hyuabot.backend.codegen.types.SubwayArrival
import app.hyuabot.backend.codegen.types.SubwayArrivalGroup
import app.hyuabot.backend.codegen.types.SubwayInput
import app.hyuabot.backend.codegen.types.SubwayOriginTerminal
import app.hyuabot.backend.codegen.types.SubwayRealtime
import app.hyuabot.backend.codegen.types.SubwayRoute
import app.hyuabot.backend.codegen.types.SubwayStation
import app.hyuabot.backend.codegen.types.SubwayStationInput
import app.hyuabot.backend.codegen.types.SubwayTimetable
import app.hyuabot.backend.database.entity.PublicHoliday
import app.hyuabot.backend.holiday.service.PublicHolidayService
import app.hyuabot.backend.subway.controller.SubwayDataFetcher
import app.hyuabot.backend.subway.domain.SubwayTimetableKey
import app.hyuabot.backend.subway.service.SubwayService
import app.hyuabot.backend.subway.service.SubwayStationNameService
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import graphql.GraphQLContext
import org.dataloader.DataLoader
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.concurrent.CompletableFuture
import kotlin.test.Test
import kotlin.test.assertEquals

class SubwayDataFetcherEdgeTest {
    private val subwayService = mock<SubwayService>()
    private val publicHolidayService = mock<PublicHolidayService>()
    private val stationNameService = mock<SubwayStationNameService>()
    private val station =
        SubwayStation(
            stationID = "S1",
            name = "Station",
            order = 1,
            minutes = 0,
            route = SubwayRoute(seq = 4, name = "Line 4"),
            realtime = emptyList(),
            timetable = emptyList(),
            arrival = emptyList(),
        )
    private val terminal = SubwayOriginTerminal(stationID = "T1", name = "Terminal")

    @Test
    fun `arrival handles empty and ambiguous weekday filters`() {
        val fetcher = fetcher(at("2026-10-05T14:50:00Z"))

        val noWeekdays = environment(fetcher, input(weekdays = emptyList()))
        assertEquals(emptyList(), fetcher.arrival(noWeekdays))

        val multipleWeekdays = environment(fetcher, input(weekdays = listOf("weekdays", "weekends")))
        assertThrows<IllegalArgumentException> { fetcher.arrival(multipleWeekdays) }
    }

    @Test
    fun `arrival applies holiday date after time filtering and limit`() {
        val fetcher = fetcher(at("2026-10-05T14:50:00Z"))
        whenever(publicHolidayService.findPublicHoliday(LocalDate.of(2026, 10, 5)))
            .thenReturn(PublicHoliday(date = LocalDate.of(2026, 10, 5), name = "holiday", calendarType = "SOLAR"))
        whenever(subwayService.getArrival(eq("S1"), eq(listOf("up", "0")), eq("weekends"), isNull(), any()))
            .thenReturn(
                listOf(
                    SubwayArrivalGroup(
                        direction = "up",
                        entries =
                            listOf(
                                arrival(4, realtime = true, trainNumber = "R1"),
                                arrival(20, realtime = true, trainNumber = "R2"),
                                arrival(10, realtime = false, trainNumber = "T1"),
                                arrival(30, realtime = false, trainNumber = "T2"),
                            ),
                    ),
                ),
            )

        val result = fetcher.arrival(environment(fetcher, input(limit = 2, after = LocalTime.of(0, 10))))

        assertEquals(listOf(20, 30), result.single().entries.map { it.minutes })
    }

    @Test
    fun `arrival uses requested weekday when it is not a holiday`() {
        val fetcher = fetcher(at("2026-10-05T14:50:00Z"))
        whenever(publicHolidayService.findPublicHoliday(LocalDate.of(2026, 10, 5))).thenReturn(null)
        whenever(subwayService.getArrival(eq("S1"), eq(listOf("up", "0")), eq("weekdays"), isNull(), any()))
            .thenReturn(listOf(SubwayArrivalGroup("up", listOf(arrival(7, realtime = false, trainNumber = "T1")))))

        val result = fetcher.arrival(environment(fetcher, input()))

        assertEquals(listOf(7), result.single().entries.map { it.minutes })
    }

    @Test
    fun `timetable filters entries before the requested time`() {
        val fetcher = fetcher(at("2026-10-05T14:50:00Z"))
        val dataFetchingEnvironment = environment(fetcher, input(after = LocalTime.of(6, 0)))
        val dataLoader = mock<DataLoader<SubwayTimetableKey, List<SubwayTimetable>>>()
        val key = SubwayTimetableKey("S1", listOf("up"), listOf("weekdays"))
        whenever(dataFetchingEnvironment.getDataLoader<SubwayTimetableKey, List<SubwayTimetable>>("subwayTimetableDataLoader"))
            .thenReturn(dataLoader)
        whenever(dataLoader.load(key))
            .thenReturn(
                CompletableFuture.completedFuture(
                    listOf(
                        timetable("05:00"),
                        timetable("06:00"),
                        timetable("07:00"),
                    ),
                ),
            )

        val result = fetcher.timetable(dataFetchingEnvironment).join()

        assertEquals(listOf(LocalTime.of(6, 0), LocalTime.of(7, 0)), result.map { it.time })

        val unfilteredEnvironment = environment(fetcher, input())
        val unfilteredLoader = mock<DataLoader<SubwayTimetableKey, List<SubwayTimetable>>>()
        whenever(unfilteredEnvironment.getDataLoader<SubwayTimetableKey, List<SubwayTimetable>>("subwayTimetableDataLoader"))
            .thenReturn(unfilteredLoader)
        whenever(unfilteredLoader.load(key)).thenReturn(CompletableFuture.completedFuture(listOf(timetable("05:00"))))

        assertEquals(listOf(LocalTime.of(5, 0)), fetcher.timetable(unfilteredEnvironment).join().map { it.time })
    }

    private fun fetcher(clock: Clock): SubwayDataFetcher =
        SubwayDataFetcher(subwayService, publicHolidayService, stationNameService, clock)

    private fun environment(
        fetcher: SubwayDataFetcher,
        input: SubwayInput,
    ): DgsDataFetchingEnvironment {
        whenever(subwayService.getStationViews(listOf("S1"))).thenReturn(listOf(station))
        val localContext = fetcher.subway(input).localContext
        return mock<DgsDataFetchingEnvironment>().also { environment ->
            whenever(environment.getSource<SubwayStation>()).thenReturn(station)
            whenever(environment.getLocalContext<Any>()).thenReturn(localContext)
            whenever(environment.graphQlContext).thenReturn(GraphQLContext.newContext().build())
        }
    }

    private fun input(
        weekdays: List<String> = listOf("weekdays"),
        limit: Int? = null,
        after: LocalTime? = null,
    ): SubwayInput =
        SubwayInput(
            keys =
                listOf(
                    SubwayStationInput(
                        stationID = "S1",
                        direction = listOf("up"),
                        weekdays = weekdays,
                        limit = limit,
                        after = after,
                    ),
                ),
        )

    private fun arrival(
        minutes: Int,
        realtime: Boolean,
        trainNumber: String?,
    ): SubwayArrival =
        SubwayArrival(
            minutes = minutes,
            terminal = terminal,
            isRealtime = realtime,
            trainNumber = trainNumber,
        )

    private fun timetable(time: String) =
        SubwayTimetable(
            seq = 1,
            time = LocalTime.parse(time),
            weekday = "weekdays",
            direction = "up",
            origin = terminal,
            terminal = terminal,
        )

    private fun realtime(trainNumber: String) =
        SubwayRealtime(
            order = 1,
            location = "Station",
            stops = 1,
            minutes = 1,
            direction = "up",
            terminal = terminal,
            trainNumber = trainNumber,
            isExpress = false,
            isLast = false,
            status = 1,
            updatedAt = ZonedDateTime.parse("2026-10-05T12:00:00+09:00[Asia/Seoul]"),
        )

    private fun at(instant: String): Clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)
}
