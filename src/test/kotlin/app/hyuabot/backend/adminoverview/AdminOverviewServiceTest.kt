package app.hyuabot.backend.adminoverview

import app.hyuabot.backend.adminoverview.domain.CronJobRun
import app.hyuabot.backend.database.entity.AdminUserInvitation
import app.hyuabot.backend.database.entity.ShuttleHoliday
import app.hyuabot.backend.database.entity.ShuttlePeriod
import app.hyuabot.backend.database.repository.AdminUserInvitationRepository
import app.hyuabot.backend.holiday.audit.HolidayAuditIssue
import app.hyuabot.backend.holiday.audit.HolidayAuditResult
import app.hyuabot.backend.holiday.audit.HolidayAuditService
import app.hyuabot.backend.security.AdminPermission
import app.hyuabot.backend.shuttle.service.ShuttleHolidayService
import app.hyuabot.backend.shuttle.service.ShuttlePeriodService
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.ZonedDateTime
import java.util.UUID

class AdminOverviewServiceTest {
    private val prometheusClient = mock<PrometheusClient>()
    private val periodService = mock<ShuttlePeriodService>()
    private val holidayService = mock<ShuttleHolidayService>()
    private val holidayAuditService = mock<HolidayAuditService>()
    private val invitationRepository = mock<AdminUserInvitationRepository>()
    private val service =
        AdminOverviewService(
            prometheusClient,
            periodService,
            holidayService,
            holidayAuditService,
            invitationRepository,
            "https://grafana.example",
        )

    init {
        whenever(holidayAuditService.audit(any(), any())).thenAnswer { invocation ->
            HolidayAuditResult(invocation.getArgument(1), null, emptyList())
        }
    }

    @Test
    fun `overview summarizes service health shuttle holiday and expiring invitations`() {
        val now = Instant.now()
        whenever(prometheusClient.getCronJobRuns()).thenReturn(
            mapOf(
                "bus-realtime-cron-job" to CronJobRun(now.minusSeconds(60).toString(), now.minusSeconds(120).toString()),
                "subway-realtime-cron-job" to CronJobRun(now.minusSeconds(120).toString(), now.minusSeconds(60).toString()),
                "cafeteria-cron-job" to CronJobRun(now.minusSeconds(3 * 3600).toString(), null),
            ),
        )
        val current = ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone)
        whenever(periodService.findShuttlePeriod(any())).thenReturn(
            ShuttlePeriod(1, "vacation", current.minusDays(1), current.plusDays(1), null),
        )
        whenever(holidayService.findShuttleHoliday(any())).thenReturn(ShuttleHoliday(1, current.toLocalDate(), "weekends", "solar"))
        whenever(invitationRepository.findAllByConsumedAtIsNullAndRevokedAtIsNull()).thenReturn(
            listOf(
                invitation(current.plusHours(1)),
                invitation(current.minusHours(1)),
                invitation(current.plusHours(25)),
            ),
        )

        val result = service.getOverview(AdminPermission.entries.toSet())

        assertEquals("https://grafana.example", result.grafanaURL)
        assertEquals(1, result.expiringInvitationCount)
        assertEquals("NORMAL", result.services.first { it.id == "shuttle" }.status)
        assertEquals("방학 · 오늘은 주말/공휴일입니다.", result.services.first { it.id == "shuttle" }.message)
        assertEquals("NORMAL", result.services.first { it.id == "bus" }.status)
        assertEquals("ERROR", result.services.first { it.id == "subway" }.status)
        assertEquals("WARNING", result.services.first { it.id == "cafeteria" }.status)
        assertEquals("UNKNOWN", result.services.first { it.id == "reading-room" }.status)
        assertEquals("NORMAL", result.services.first { it.id == "holiday-configuration" }.status)
    }

    @Test
    fun `missing periods and Prometheus failures remain actionable without super admin data`() {
        whenever(prometheusClient.getCronJobRuns()).thenThrow(IllegalStateException("offline"))
        whenever(periodService.findShuttlePeriod(any())).thenReturn(null)
        whenever(holidayService.findShuttleHoliday(any())).thenReturn(null)

        val result = service.getOverview(setOf(AdminPermission.SHUTTLE, AdminPermission.BUS))

        assertNull(result.expiringInvitationCount)
        assertEquals("WARNING", result.services.first { it.id == "shuttle" }.status)
        assertEquals("UNKNOWN", result.services.first { it.id == "bus" }.status)
    }

    @Test
    fun `regular shuttle days and jobs without successful runs are described`() {
        val current = ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone)
        whenever(periodService.findShuttlePeriod(any())).thenReturn(
            ShuttlePeriod(1, "semester", current.minusDays(1), current.plusDays(1), null),
        )
        whenever(holidayService.findShuttleHoliday(any())).thenReturn(null)
        whenever(prometheusClient.getCronJobRuns()).thenReturn(
            mapOf("reading-room-cron-job" to CronJobRun(null, null)),
        )

        val result = service.getOverview(setOf(AdminPermission.SHUTTLE, AdminPermission.READING_ROOM))

        assertEquals("학기 운행 기간입니다.", result.services.first { it.id == "shuttle" }.message)
        assertEquals("WARNING", result.services.first { it.id == "reading-room" }.status)
    }

    @Test
    fun `unknown period and holiday codes remain readable`() {
        val current = ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone)
        whenever(periodService.findShuttlePeriod(any())).thenReturn(
            ShuttlePeriod(1, "special", current.minusDays(1), current.plusDays(1), null),
        )
        whenever(holidayService.findShuttleHoliday(any())).thenReturn(ShuttleHoliday(1, current.toLocalDate(), "special-day", "solar"))
        whenever(prometheusClient.getCronJobRuns()).thenReturn(emptyMap())

        val result = service.getOverview(setOf(AdminPermission.SHUTTLE))

        assertEquals("special · 오늘은 special-day입니다.", result.services.first { it.id == "shuttle" }.message)
    }

    @Test
    fun `realtime jobs are normal during overnight maintenance hours`() {
        val overnight = ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone).withHour(3).withMinute(0)
        whenever(prometheusClient.getCronJobRuns()).thenReturn(
            mapOf("bus-realtime-cron-job" to CronJobRun(overnight.minusHours(3).toInstant().toString(), null)),
        )

        val result = service.getOverview(setOf(AdminPermission.BUS), overnight)

        assertEquals("NORMAL", result.services.first { it.id == "bus" }.status)
        assertEquals("현재는 실시간 수집 운영 시간 외입니다.", result.services.first { it.id == "bus" }.message)

        val earlyMorning = overnight.withHour(1)
        whenever(prometheusClient.getCronJobRuns()).thenReturn(
            mapOf("bus-realtime-cron-job" to CronJobRun(earlyMorning.minusMinutes(1).toInstant().toString(), null)),
        )
        assertEquals(
            "데이터 수집이 정상입니다.",
            service
                .getOverview(setOf(AdminPermission.BUS), earlyMorning)
                .services
                .first { it.id == "bus" }
                .message,
        )
    }

    @Test
    fun `holiday configuration card promotes imminent issues to error`() {
        val now = ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone)
        whenever(holidayAuditService.audit(any(), any())).thenReturn(
            HolidayAuditResult(
                checkedAt = now,
                lastSuccessAt = now.minusHours(1),
                issues =
                    listOf(
                        HolidayAuditIssue(
                            code = "SHUTTLE_DECISION_MISSING",
                            service = "shuttle",
                            date = now.toLocalDate(),
                            message = "설정이 필요합니다.",
                            severity = "ERROR",
                            managementPath = "/shuttle/holiday",
                        ),
                        HolidayAuditIssue(
                            code = "PUBLIC_HOLIDAY_SYNC_STALE",
                            service = "holiday",
                            date = null,
                            message = "동기화를 확인해주세요.",
                            severity = "WARNING",
                            managementPath = "/bus/holiday",
                        ),
                    ),
            ),
        )
        whenever(prometheusClient.getCronJobRuns()).thenReturn(emptyMap())

        val card = service.getOverview(setOf(AdminPermission.BUS), now).services.first { it.id == "holiday-configuration" }

        assertEquals("ERROR", card.status)
        assertEquals("임박한 휴일 설정 문제 1건과 확인 항목 1건이 있습니다.", card.message)
        assertEquals(now.minusHours(1).toString(), card.lastSuccessAt)
        assertEquals("/operations/holiday-audit", card.managementPath)
    }

    @Test
    fun `holiday configuration card distinguishes warnings and unrelated permissions`() {
        val now = ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone)
        whenever(holidayAuditService.audit(any(), any())).thenReturn(
            HolidayAuditResult(
                checkedAt = now,
                lastSuccessAt = now.minusHours(1),
                issues =
                    listOf(
                        HolidayAuditIssue(
                            code = "PUBLIC_HOLIDAY_SYNC_STALE",
                            service = "holiday",
                            date = null,
                            message = "동기화를 확인해주세요.",
                            severity = "WARNING",
                            managementPath = "/bus/holiday",
                        ),
                    ),
            ),
        )
        whenever(prometheusClient.getCronJobRuns()).thenReturn(emptyMap())

        val warning = service.getOverview(setOf(AdminPermission.BUS), now).services.first { it.id == "holiday-configuration" }
        val unrelated = service.getOverview(setOf(AdminPermission.CAFETERIA), now)

        assertEquals("WARNING", warning.status)
        assertEquals("확인이 필요한 휴일 설정이 1건 있습니다.", warning.message)
        assertEquals(listOf("cafeteria"), unrelated.services.map { it.id })
    }

    @Test
    fun `weather status follows notice permission and hourly freshness`() {
        val current = ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone)
        whenever(prometheusClient.getCronJobRuns()).thenReturn(
            mapOf("weather-cron-job" to CronJobRun(current.minusMinutes(89).toInstant().toString(), null)),
        )

        val normal = service.getOverview(setOf(AdminPermission.NOTICE), current).services.single()

        assertEquals("weather", normal.id)
        assertEquals("날씨", normal.title)
        assertEquals("NORMAL", normal.status)
        assertEquals("/notice/notice", normal.managementPath)

        whenever(prometheusClient.getCronJobRuns()).thenReturn(
            mapOf("weather-cron-job" to CronJobRun(current.minusMinutes(91).toInstant().toString(), null)),
        )

        assertEquals(
            "WARNING",
            service
                .getOverview(setOf(AdminPermission.NOTICE), current)
                .services
                .single()
                .status,
        )
    }

    private fun invitation(expiresAt: ZonedDateTime) =
        AdminUserInvitation(
            uuid = UUID.randomUUID(),
            userID = "user",
            tokenHash = "hash-${UUID.randomUUID()}",
            createdBy = "admin",
            expiresAt = expiresAt,
            createdAt = expiresAt.minusDays(1),
        )
}
