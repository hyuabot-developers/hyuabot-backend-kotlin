package app.hyuabot.backend.holiday

import app.hyuabot.backend.database.entity.HolidaySyncState
import app.hyuabot.backend.database.entity.PublicHoliday
import app.hyuabot.backend.database.entity.ShuttleHoliday
import app.hyuabot.backend.database.repository.HolidaySyncStateRepository
import app.hyuabot.backend.database.repository.PublicHolidayRepository
import app.hyuabot.backend.database.repository.ShuttleHolidayRepository
import app.hyuabot.backend.database.repository.ShuttleTimetableRepository
import app.hyuabot.backend.holiday.audit.HolidayAuditService
import app.hyuabot.backend.security.AdminPermission
import app.hyuabot.backend.shuttle.service.ShuttlePeriodService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@ExtendWith(MockitoExtension::class)
class HolidayAuditServiceTest {
    @Mock private lateinit var syncStateRepository: HolidaySyncStateRepository

    @Mock private lateinit var publicHolidayRepository: PublicHolidayRepository

    @Mock private lateinit var shuttleHolidayRepository: ShuttleHolidayRepository

    @Mock private lateinit var shuttleTimetableRepository: ShuttleTimetableRepository

    @Mock private lateinit var shuttlePeriodService: ShuttlePeriodService

    private val now = ZonedDateTime.of(2026, 7, 17, 9, 0, 0, 0, ZoneId.of("Asia/Seoul"))

    private fun service() =
        HolidayAuditService(
            syncStateRepository,
            publicHolidayRepository,
            shuttleHolidayRepository,
            shuttleTimetableRepository,
            shuttlePeriodService,
        )

    @Test
    fun `super admin receives sync and shuttle issues`() {
        val imminent = now.toLocalDate().plusDays(2)
        val later = now.toLocalDate().plusDays(10)
        whenever(syncStateRepository.findBySource("KASI")).thenReturn(null)
        whenever(shuttleHolidayRepository.findAll()).thenReturn(
            listOf(ShuttleHoliday(1, LocalDate.of(2026, 1, 1), "legacy", "gregorian")),
        )
        whenever(
            publicHolidayRepository.findOfficialHolidaysBetween(
                "KASI",
                "solar",
                now.toLocalDate(),
                now.toLocalDate().plusDays(90),
            ),
        ).thenReturn(
            listOf(
                PublicHoliday(1, imminent, "제헌절", "solar"),
                PublicHoliday(2, later, "광복절", "solar"),
            ),
        )
        whenever(shuttleHolidayRepository.findByDateAndCalendarType(imminent, "solar")).thenReturn(null)
        whenever(shuttleHolidayRepository.findByDateAndCalendarType(later, "solar")).thenReturn(
            ShuttleHoliday(2, later, "weekends", "solar"),
        )
        whenever(shuttlePeriodService.findShuttlePeriod(imminent)).thenReturn(null)
        whenever(shuttlePeriodService.findShuttlePeriod(later)).thenReturn(
            app.hyuabot.backend.database.entity.ShuttlePeriod(
                1,
                "vacation",
                now.minusDays(1),
                now.plusDays(30),
                null,
            ),
        )
        whenever(shuttleTimetableRepository.existsByPeriodTypeAndWeekday("vacation", false)).thenReturn(false)
        val result = service().audit(setOf(AdminPermission.SUPER_ADMIN), now)

        assertEquals(now, result.checkedAt)
        assertEquals(null, result.lastSuccessAt)
        assertEquals(
            setOf(
                "PUBLIC_HOLIDAY_SYNC_STALE",
                "SHUTTLE_DECISION_INVALID",
                "SHUTTLE_DECISION_MISSING",
                "SHUTTLE_PERIOD_MISSING",
                "SHUTTLE_WEEKEND_TIMETABLE_EMPTY",
            ),
            result.issues.map { it.code }.toSet(),
        )
        assertTrue(result.issues.filter { it.date == imminent }.all { it.severity == "ERROR" })
        assertTrue(result.issues.filter { it.date == later }.all { it.severity == "WARNING" })
        assertEquals("/bus/holiday", result.issues.first { it.code == "PUBLIC_HOLIDAY_SYNC_STALE" }.managementPath)
    }

    @Test
    fun `fresh sync and halted shuttle holiday require no corrective action`() {
        val date = now.toLocalDate().plusDays(5)
        val syncState =
            HolidaySyncState(
                source = "KASI",
                lastAttemptAt = now.minusHours(1),
                lastSuccessAt = now.minusHours(1),
                rangeStart = now.toLocalDate(),
                rangeEnd = now.toLocalDate().plusYears(1),
                lastError = null,
            )
        whenever(syncStateRepository.findBySource("KASI")).thenReturn(syncState)
        whenever(shuttleHolidayRepository.findAll()).thenReturn(emptyList())
        whenever(publicHolidayRepository.findOfficialHolidaysBetween(any(), any(), any(), any())).thenReturn(
            listOf(PublicHoliday(1, date, "광복절", "solar")),
        )
        whenever(shuttleHolidayRepository.findByDateAndCalendarType(date, "solar")).thenReturn(
            ShuttleHoliday(1, date, "halt", "solar"),
        )
        whenever(shuttlePeriodService.findShuttlePeriod(date)).thenReturn(
            app.hyuabot.backend.database.entity
                .ShuttlePeriod(1, "semester", now.minusDays(1), now.plusDays(30), null),
        )

        val result = service().audit(setOf(AdminPermission.SHUTTLE), now)

        assertTrue(result.issues.isEmpty())
        assertEquals(syncState.lastSuccessAt, result.lastSuccessAt)
        verify(shuttleTimetableRepository, never()).existsByPeriodTypeAndWeekday(any(), any())
    }

    @Test
    fun `permissions limit audit scope and select available management path`() {
        whenever(syncStateRepository.findBySource("KASI")).thenReturn(null)
        val subway = service().audit(setOf(AdminPermission.SUBWAY), now)
        val shuttle = service().audit(setOf(AdminPermission.SHUTTLE), now)
        val unrelated = service().audit(setOf(AdminPermission.CAFETERIA), now)

        assertEquals("/subway/holiday", subway.issues.single().managementPath)
        assertEquals("/shuttle/holiday", shuttle.issues.single().managementPath)
        assertTrue(unrelated.issues.isEmpty())
    }
}
