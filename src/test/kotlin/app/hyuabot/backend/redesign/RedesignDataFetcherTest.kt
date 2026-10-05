package app.hyuabot.backend.redesign

import app.hyuabot.backend.codegen.types.SubwayRoute
import app.hyuabot.backend.codegen.types.SubwayStation
import app.hyuabot.backend.database.entity.PublicHoliday
import app.hyuabot.backend.database.entity.SubwayAlert
import app.hyuabot.backend.database.entity.SubwayStationFacility
import app.hyuabot.backend.database.entity.SubwayTimetable
import app.hyuabot.backend.database.repository.SubwayAlertRepository
import app.hyuabot.backend.database.repository.SubwayStationFacilityRepository
import app.hyuabot.backend.database.repository.SubwayTimetableRepository
import app.hyuabot.backend.holiday.service.PublicHolidayService
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class RedesignDataFetcherTest {
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
    private val facilityRepository = mock<SubwayStationFacilityRepository>()
    private val timetableRepository = mock<SubwayTimetableRepository>()
    private val alertRepository = mock<SubwayAlertRepository>()
    private val publicHolidayService = mock<PublicHolidayService>()

    @Test
    fun `facilities map every stored field`() {
        whenever(facilityRepository.findByStationIDOrderByFacilityTypeAscSortOrderAsc("S1"))
            .thenReturn(listOf(SubwayStationFacility(3, "S1", "elevator", 2, "1", "north", "platform", "설명", "description", "source")))

        val result = fetcher(at("2026-10-05T03:00:00Z")).facilities(environment())

        assertEquals(
            listOf(
                mapOf(
                    "type" to "elevator",
                    "order" to 2,
                    "exitNumber" to "1",
                    "fromPlace" to "north",
                    "toPlace" to "platform",
                    "descriptionKorean" to "설명",
                    "descriptionEnglish" to "description",
                    "source" to "source",
                ),
            ),
            result,
        )
    }

    @Test
    fun `fetcher defaults to the system clock when no clock is injected`() {
        whenever(facilityRepository.findByStationIDOrderByFacilityTypeAscSortOrderAsc("S1")).thenReturn(emptyList())

        val fetcher = RedesignDataFetcher(facilityRepository, timetableRepository, alertRepository, publicHolidayService)

        assertEquals(emptyList(), fetcher.facilities(environment()))
    }

    @Test
    fun `first and last trains use service date and weekday data`() {
        whenever(publicHolidayService.findPublicHoliday(any())).thenReturn(null)
        whenever(timetableRepository.findByStationID("S1"))
            .thenReturn(
                listOf(
                    timetable("weekdays", "0", "05:00"),
                    timetable("weekdays", "0", "06:00"),
                    timetable("weekdays", "1", "07:00"),
                    timetable("weekdays", "2", "08:00"),
                    timetable("weekdays", "3", "09:00"),
                    timetable("weekdays", "3", "08:00"),
                    timetable("weekends", "0", "09:00"),
                ),
            )

        val result = fetcher(at("2026-10-05T03:00:00Z")).firstLast(environment())

        assertEquals(
            listOf(
                mapOf(
                    "direction" to "2",
                    "weekday" to "weekdays",
                    "first" to LocalTime.parse("08:00"),
                    "last" to LocalTime.parse("08:00"),
                ),
                mapOf(
                    "direction" to "3",
                    "weekday" to "weekdays",
                    "first" to LocalTime.parse("08:00"),
                    "last" to LocalTime.parse("09:00"),
                ),
                mapOf(
                    "direction" to "down",
                    "weekday" to "weekdays",
                    "first" to LocalTime.parse("07:00"),
                    "last" to LocalTime.parse("07:00"),
                ),
                mapOf(
                    "direction" to "up",
                    "weekday" to "weekdays",
                    "first" to LocalTime.parse("05:00"),
                    "last" to LocalTime.parse("06:00"),
                ),
            ),
            result,
        )
        verify(publicHolidayService).findPublicHoliday(LocalDate.of(2026, 10, 5))
    }

    @Test
    fun `empty timetable groups have no first or last time`() {
        assertEquals(null to null, subwayFirstAndLast(emptyList()))
    }

    @Test
    fun `first and last trains treat pre four am as the prior service date`() {
        whenever(timetableRepository.findByStationID("S1")).thenReturn(listOf(timetable("weekends", "0", "00:30")))

        val result = fetcher(at("2026-10-04T18:00:00Z")).firstLast(environment())

        assertEquals("up", result.single()["direction"])
        assertEquals("weekends", result.single()["weekday"])
        verify(publicHolidayService, never()).findPublicHoliday(any())
    }

    @Test
    fun `first and last trains use weekend schedule on a weekday holiday`() {
        whenever(publicHolidayService.findPublicHoliday(LocalDate.of(2026, 10, 5)))
            .thenReturn(PublicHoliday(date = LocalDate.of(2026, 10, 5), name = "holiday", calendarType = "SOLAR"))
        whenever(timetableRepository.findByStationID("S1")).thenReturn(listOf(timetable("weekends", "1", "06:00")))

        val result = fetcher(at("2026-10-05T03:00:00Z")).firstLast(environment())

        assertEquals("down", result.single()["direction"])
        assertEquals("weekends", result.single()["weekday"])
    }

    @Test
    fun `alerts include global and active route alerts only`() {
        val now = ZonedDateTime.parse("2026-10-05T12:00:00+09:00[Asia/Seoul]")
        whenever(alertRepository.findAll())
            .thenReturn(
                listOf(
                    SubwayAlert("global", null, "Global", null, null, null, "source", now),
                    SubwayAlert("global-window", null, "Global window", "content", now.minusMinutes(2), now.plusMinutes(2), "source", now),
                    SubwayAlert("global-future", null, "Global future", "content", now.plusMinutes(1), now.plusHours(1), "source", now),
                    SubwayAlert("global-expired", null, "Global expired", "content", null, now, "source", now),
                    SubwayAlert("active", 4, "Active", "content", now.minusHours(1), now.plusHours(1), "source", now),
                    SubwayAlert("no-start", 4, "No start", "content", null, now.plusHours(1), "source", now),
                    SubwayAlert("no-end", 4, "No end", "content", now.minusHours(2), null, "source", now),
                    SubwayAlert("future", 4, "Future", "content", now.plusMinutes(1), null, "source", now),
                    SubwayAlert("expired", 4, "Expired", "content", null, now, "source", now),
                    SubwayAlert("other-route", 9, "Other route", "content", null, null, "source", now),
                ),
            )

        val result = fetcher(at("2026-10-05T03:00:00Z")).alerts(environment())

        assertEquals(setOf("active", "global", "global-window", "no-start", "no-end"), result.map { it["id"] }.toSet())
        val active = result.single { it["id"] == "active" }
        assertEquals(4, active["routeID"])
        assertEquals("Active", active["title"])
        assertEquals("content", active["content"])
        assertEquals(now.minusHours(1), active["startsAt"])
        assertEquals(now.plusHours(1), active["endsAt"])
        assertEquals("source", active["source"])
    }

    @Test
    fun `alerts handle a route id that becomes null between proxy reads`() {
        val proxyAlert = mock<SubwayAlert>()
        whenever(proxyAlert.routeID).thenReturn(4, null)
        whenever(alertRepository.findAll()).thenReturn(listOf(proxyAlert))

        val result = fetcher(at("2026-10-05T03:00:00Z")).alerts(environment())

        assertEquals(emptyList(), result)
    }

    private fun fetcher(clock: Clock) =
        RedesignDataFetcher(facilityRepository, timetableRepository, alertRepository, publicHolidayService, clock)

    private fun environment(): DgsDataFetchingEnvironment =
        mock<DgsDataFetchingEnvironment>().also { whenever(it.getSource<SubwayStation>()).thenReturn(station) }

    private fun timetable(
        weekday: String,
        heading: String,
        departureTime: String,
    ): SubwayTimetable =
        SubwayTimetable(
            stationID = "S1",
            startStationID = "start",
            terminalStationID = "terminal",
            departureTime = LocalTime.parse(departureTime),
            weekday = weekday,
            heading = heading,
            station = null,
            startStation = null,
            terminalStation = null,
        )

    private fun at(instant: String): Clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)
}
