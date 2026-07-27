package app.hyuabot.backend.presence

import app.hyuabot.backend.database.entity.PublicHoliday
import app.hyuabot.backend.database.entity.ShuttleHoliday
import app.hyuabot.backend.database.entity.ShuttlePeriod
import app.hyuabot.backend.holiday.service.PublicHolidayService
import app.hyuabot.backend.shuttle.domain.ShuttleTimetableKey
import app.hyuabot.backend.shuttle.domain.ShuttleTimetableResult
import app.hyuabot.backend.shuttle.domain.ShuttleTimetableViewItem
import app.hyuabot.backend.shuttle.service.ShuttleHolidayService
import app.hyuabot.backend.shuttle.service.ShuttlePeriodService
import app.hyuabot.backend.shuttle.service.ShuttleTimetableService
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShuttleDemandWindowServiceTest {
    private val timetableService = mock<ShuttleTimetableService>()
    private val holidayService = mock<ShuttleHolidayService>()
    private val publicHolidayService = mock<PublicHolidayService>()
    private val periodService = mock<ShuttlePeriodService>()
    private val service =
        ShuttleDemandWindowService(
            timetableService,
            holidayService,
            publicHolidayService,
            periodService,
        )

    // 2026-07-21 is a Tuesday; 03:00Z == 12:00 Asia/Seoul.
    private val now = Instant.parse("2026-07-21T03:00:00Z")
    private val today = LocalDate.of(2026, 7, 21)

    private fun epochOf(time: LocalTime) = ZonedDateTime.of(today, time, LocalDateTimeBuilder.serviceTimezone).toEpochSecond()

    private fun item(time: LocalTime) =
        ShuttleTimetableViewItem(
            seq = time.toSecondOfDay(),
            routeName = "DH",
            routeTag = "DH",
            period = "semester",
            weekday = true,
            time = time,
            group = "STATION",
            stops = emptyList(),
        )

    private fun periodMock(type: String = "semester"): ShuttlePeriod {
        val period = mock<ShuttlePeriod>()
        whenever(period.type).thenReturn(type)
        return period
    }

    private fun holidayMock(type: String): ShuttleHoliday {
        val holiday = mock<ShuttleHoliday>()
        whenever(holiday.type).thenReturn(type)
        return holiday
    }

    private fun stubSchedule(timesByStop: Map<String, List<LocalTime>>) {
        whenever(timetableService.getShuttleTimetableBatch(any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val keys = invocation.getArgument(0) as Set<ShuttleTimetableKey>
            keys.associateWith { key ->
                ShuttleTimetableResult(
                    order = (timesByStop[key.stop] ?: emptyList()).map { item(it) },
                    destination = emptyMap(),
                )
            }
        }
    }

    private fun stubOperatingWeekday() {
        val period = periodMock()
        whenever(periodService.findShuttlePeriod(any())).thenReturn(period)
        whenever(holidayService.findShuttleHoliday(any())).thenReturn(null)
        whenever(publicHolidayService.findPublicHoliday(any())).thenReturn(null)
    }

    @Test
    fun `computes the window from the previous departure until the next one`() {
        stubOperatingWeekday()
        stubSchedule(mapOf("station" to listOf(LocalTime.of(11, 0), LocalTime.of(11, 30), LocalTime.of(13, 0))))

        val window = service.demandWindow("station", now)!!

        assertEquals(epochOf(LocalTime.of(11, 30)), window.startEpoch)
        assertEquals((epochOf(LocalTime.of(13, 0)) - now.epochSecond) + 120, window.keyTtlSeconds)
    }

    @Test
    fun `uses the start of day when there is no earlier departure`() {
        stubOperatingWeekday()
        stubSchedule(mapOf("station" to listOf(LocalTime.of(13, 0), LocalTime.of(14, 0))))

        val window = service.demandWindow("station", now)!!

        assertEquals(epochOf(LocalTime.MIDNIGHT), window.startEpoch)
    }

    @Test
    fun `returns null after the last departure of the day`() {
        stubOperatingWeekday()
        stubSchedule(mapOf("station" to listOf(LocalTime.of(8, 0), LocalTime.of(9, 0))))

        assertNull(service.demandWindow("station", now))
    }

    @Test
    fun `returns null when the date is in no operating period`() {
        whenever(periodService.findShuttlePeriod(any())).thenReturn(null)

        assertNull(service.demandWindow("station", now))
    }

    @Test
    fun `runs a weekend schedule on a shuttle weekend-operation holiday`() {
        val period = periodMock()
        val holiday = holidayMock("weekends")
        whenever(periodService.findShuttlePeriod(any())).thenReturn(period)
        whenever(holidayService.findShuttleHoliday(any())).thenReturn(holiday)
        stubSchedule(mapOf("station" to listOf(LocalTime.of(11, 0), LocalTime.of(13, 0))))

        val window = service.demandWindow("station", now)!!

        assertEquals(epochOf(LocalTime.of(11, 0)), window.startEpoch)
    }

    @Test
    fun `returns null when the shuttle is halted for a holiday`() {
        val period = periodMock()
        val holiday = holidayMock("halt")
        whenever(periodService.findShuttlePeriod(any())).thenReturn(period)
        whenever(holidayService.findShuttleHoliday(any())).thenReturn(holiday)

        assertNull(service.demandWindow("station", now))
    }

    @Test
    fun `runs a weekend schedule on a public holiday`() {
        val period = periodMock()
        val publicHoliday = mock<PublicHoliday>()
        whenever(periodService.findShuttlePeriod(any())).thenReturn(period)
        whenever(holidayService.findShuttleHoliday(any())).thenReturn(null)
        whenever(publicHolidayService.findPublicHoliday(any())).thenReturn(publicHoliday)
        stubSchedule(mapOf("station" to listOf(LocalTime.of(11, 0), LocalTime.of(13, 0))))

        val window = service.demandWindow("station", now)!!

        assertEquals(epochOf(LocalTime.of(11, 0)), window.startEpoch)
    }

    @Test
    fun `caches the daily schedule so it is computed once per date`() {
        stubOperatingWeekday()
        stubSchedule(mapOf("station" to listOf(LocalTime.of(11, 0), LocalTime.of(13, 0))))

        service.demandWindow("station", now)
        service.demandWindow("station", now)

        verify(timetableService, times(1)).getShuttleTimetableBatch(any())
    }
}
