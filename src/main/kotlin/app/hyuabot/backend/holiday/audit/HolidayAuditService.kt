package app.hyuabot.backend.holiday.audit

import app.hyuabot.backend.database.repository.HolidaySyncStateRepository
import app.hyuabot.backend.database.repository.PublicHolidayRepository
import app.hyuabot.backend.database.repository.ShuttleHolidayRepository
import app.hyuabot.backend.database.repository.ShuttleTimetableRepository
import app.hyuabot.backend.security.AdminPermission
import app.hyuabot.backend.shuttle.service.ShuttlePeriodService
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Service
class HolidayAuditService(
    private val syncStateRepository: HolidaySyncStateRepository,
    private val publicHolidayRepository: PublicHolidayRepository,
    private val shuttleHolidayRepository: ShuttleHolidayRepository,
    private val shuttleTimetableRepository: ShuttleTimetableRepository,
    private val shuttlePeriodService: ShuttlePeriodService,
) {
    fun audit(
        permissions: Set<AdminPermission>,
        now: ZonedDateTime = ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone),
    ): HolidayAuditResult {
        val effectivePermissions =
            if (AdminPermission.SUPER_ADMIN in permissions) AdminPermission.entries.toSet() else permissions
        val syncState = syncStateRepository.findBySource(SOURCE)
        val issues = mutableListOf<HolidayAuditIssue>()
        val hasTransportPermission =
            effectivePermissions.any { it == AdminPermission.SHUTTLE || it == AdminPermission.BUS || it == AdminPermission.SUBWAY }
        if (hasTransportPermission && (syncState?.lastSuccessAt == null || Duration.between(syncState.lastSuccessAt, now) > MAX_SYNC_AGE)) {
            issues +=
                issue(
                    code = "PUBLIC_HOLIDAY_SYNC_STALE",
                    service = "holiday",
                    message = "공식 공휴일 동기화가 36시간 이상 완료되지 않았습니다.",
                    path = publicHolidayPath(effectivePermissions),
                )
        }
        if (AdminPermission.SHUTTLE in effectivePermissions) {
            issues += auditShuttle(now.toLocalDate())
        }
        return HolidayAuditResult(now, syncState?.lastSuccessAt, issues.sortedWith(compareBy({ it.date }, { it.code }, { it.message })))
    }

    private fun auditShuttle(today: LocalDate): List<HolidayAuditIssue> {
        val issues = mutableListOf<HolidayAuditIssue>()
        val end = today.plusDays(AUDIT_DAYS)
        shuttleHolidayRepository.findAll().forEach { holiday ->
            if (holiday.type !in SHUTTLE_TYPES || holiday.calendarType !in CALENDAR_TYPES) {
                issues +=
                    issue(
                        code = "SHUTTLE_DECISION_INVALID",
                        service = "shuttle",
                        date = holiday.date,
                        message = "${holiday.date} 셔틀 휴일 설정값을 확인해주세요.",
                        path = "/shuttle/holiday",
                        today = today,
                    )
            }
        }
        val checkedWeekendPeriods = mutableSetOf<String>()
        publicHolidayRepository
            .findOfficialHolidaysBetween(SOURCE, "solar", today, end)
            .forEach { publicHoliday ->
                val decision = shuttleHolidayRepository.findByDateAndCalendarType(publicHoliday.date, "solar")
                if (decision == null) {
                    issues +=
                        issue(
                            code = "SHUTTLE_DECISION_MISSING",
                            service = "shuttle",
                            date = publicHoliday.date,
                            message = "${publicHoliday.date} ${publicHoliday.name}의 셔틀 운행 방식을 결정해주세요.",
                            path = "/shuttle/holiday",
                            today = today,
                        )
                }
                val period = shuttlePeriodService.findShuttlePeriod(publicHoliday.date)
                if (period == null) {
                    issues +=
                        issue(
                            code = "SHUTTLE_PERIOD_MISSING",
                            service = "shuttle",
                            date = publicHoliday.date,
                            message = "${publicHoliday.date}에 적용되는 셔틀 운행 기간이 없습니다.",
                            path = "/shuttle/period",
                            today = today,
                        )
                } else if (decision?.type != "halt" &&
                    checkedWeekendPeriods.add(period.type) &&
                    !shuttleTimetableRepository.existsByPeriodTypeAndWeekday(period.type, false)
                ) {
                    issues +=
                        issue(
                            code = "SHUTTLE_WEEKEND_TIMETABLE_EMPTY",
                            service = "shuttle",
                            date = publicHoliday.date,
                            message = "${period.type} 기간에 적용할 셔틀 주말 시간표가 없습니다.",
                            path = "/shuttle/timetable",
                            today = today,
                        )
                }
            }
        return issues
    }

    private fun issue(
        code: String,
        service: String,
        message: String,
        path: String,
        date: LocalDate? = null,
        today: LocalDate? = null,
    ) = HolidayAuditIssue(
        code = code,
        service = service,
        date = date,
        message = message,
        severity = if (date != null && today != null && ChronoUnit.DAYS.between(today, date) <= CRITICAL_DAYS) "ERROR" else "WARNING",
        managementPath = path,
    )

    private fun publicHolidayPath(permissions: Set<AdminPermission>) =
        when {
            AdminPermission.BUS in permissions -> "/bus/holiday"
            AdminPermission.SUBWAY in permissions -> "/subway/holiday"
            else -> "/shuttle/holiday"
        }

    companion object {
        private const val SOURCE = "KASI"
        private const val AUDIT_DAYS = 90L
        private const val CRITICAL_DAYS = 3L
        private val MAX_SYNC_AGE = Duration.ofHours(36)
        private val SHUTTLE_TYPES = setOf("weekends", "halt")
        private val CALENDAR_TYPES = setOf("solar", "lunar")
    }
}
