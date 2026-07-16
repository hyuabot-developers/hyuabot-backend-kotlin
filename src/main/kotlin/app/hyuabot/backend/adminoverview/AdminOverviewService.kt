package app.hyuabot.backend.adminoverview

import app.hyuabot.backend.adminoverview.domain.AdminOverviewResponse
import app.hyuabot.backend.adminoverview.domain.AdminServiceStatus
import app.hyuabot.backend.adminoverview.domain.CronJobRun
import app.hyuabot.backend.database.repository.AdminUserInvitationRepository
import app.hyuabot.backend.security.AdminPermission
import app.hyuabot.backend.shuttle.service.ShuttleHolidayService
import app.hyuabot.backend.shuttle.service.ShuttlePeriodService
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

@Service
class AdminOverviewService(
    private val prometheusClient: PrometheusClient,
    private val shuttlePeriodService: ShuttlePeriodService,
    private val shuttleHolidayService: ShuttleHolidayService,
    private val invitationRepository: AdminUserInvitationRepository,
    @param:Value("\${admin.overview.grafana-url:https://grafana.hyuabot.app}") private val grafanaURL: String,
) {
    fun getOverview(permissions: Set<AdminPermission>): AdminOverviewResponse =
        getOverview(permissions, ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone))

    internal fun getOverview(
        permissions: Set<AdminPermission>,
        now: ZonedDateTime,
    ): AdminOverviewResponse {
        val jobs = runCatching { prometheusClient.getCronJobRuns() }.getOrNull()
        val services =
            buildList {
                if (AdminPermission.SHUTTLE in permissions) add(shuttleStatus(now.toLocalDate()))
                JOBS.filter { it.permission in permissions }.forEach { definition ->
                    add(jobStatus(definition, jobs?.get(definition.jobName), now.toInstant()))
                }
            }
        val expiringInvitations =
            if (AdminPermission.SUPER_ADMIN in permissions) {
                invitationRepository
                    .findAllByConsumedAtIsNullAndRevokedAtIsNull()
                    .count { invitation -> invitation.expiresAt.isAfter(now) && !invitation.expiresAt.isAfter(now.plusHours(24)) }
            } else {
                null
            }
        return AdminOverviewResponse(
            checkedAt = now.toString(),
            services = services,
            expiringInvitationCount = expiringInvitations,
            grafanaURL = grafanaURL,
        )
    }

    private fun shuttleStatus(date: LocalDate): AdminServiceStatus {
        val period = shuttlePeriodService.findShuttlePeriod(date)
        val holiday = shuttleHolidayService.findShuttleHoliday(date)
        val status = if (period == null) "WARNING" else "NORMAL"
        val periodLabel = period?.type?.let { PERIOD_LABELS[it] ?: it }
        val message =
            when {
                period == null -> "현재 날짜에 적용되는 운행 기간이 없습니다."
                holiday != null -> "$periodLabel · 오늘은 ${HOLIDAY_LABELS[holiday.type] ?: holiday.type}입니다."
                else -> "$periodLabel 운행 기간입니다."
            }
        return AdminServiceStatus(
            id = "shuttle",
            title = "셔틀버스",
            status = status,
            message = message,
            lastSuccessAt = null,
            lastFailureAt = null,
            managementPath = "/shuttle/period",
        )
    }

    private fun jobStatus(
        definition: JobDefinition,
        run: CronJobRun?,
        now: Instant,
    ): AdminServiceStatus {
        val success = run?.lastSuccessAt?.let(Instant::parse)
        val failure = run?.lastFailureAt?.let(Instant::parse)
        val outsideServiceHours = definition.serviceHoursOnly.and(now.atZone(LocalDateTimeBuilder.serviceTimezone).hour in 2..4)
        val status =
            when {
                run == null -> "UNKNOWN"
                failure != null && (success == null || failure.isAfter(success)) -> "ERROR"
                success == null -> "WARNING"
                !outsideServiceHours && Duration.between(success, now) > definition.staleAfter -> "WARNING"
                else -> "NORMAL"
            }
        val message =
            when (status) {
                "ERROR" -> "최근 데이터 수집 작업이 실패했습니다."
                "WARNING" -> "최근 데이터 갱신 시각을 확인해주세요."
                "UNKNOWN" -> "수집 작업 상태를 확인할 수 없습니다."
                else -> if (outsideServiceHours) "현재는 실시간 수집 운영 시간 외입니다." else "데이터 수집이 정상입니다."
            }
        return AdminServiceStatus(
            id = definition.id,
            title = definition.title,
            status = status,
            message = message,
            lastSuccessAt = run?.lastSuccessAt,
            lastFailureAt = run?.lastFailureAt,
            managementPath = definition.managementPath,
        )
    }

    private data class JobDefinition(
        val id: String,
        val title: String,
        val permission: AdminPermission,
        val jobName: String,
        val staleAfter: Duration,
        val managementPath: String,
        val serviceHoursOnly: Boolean = false,
    )

    companion object {
        private val PERIOD_LABELS =
            mapOf(
                "semester" to "학기",
                "vacation_session" to "계절학기",
                "vacation" to "방학",
            )
        private val HOLIDAY_LABELS =
            mapOf(
                "weekends" to "주말/공휴일",
                "halt" to "운행 중지일",
            )
        private val JOBS =
            listOf(
                JobDefinition("bus", "노선버스", AdminPermission.BUS, "bus-realtime-cron-job", Duration.ofMinutes(10), "/bus/realtime", true),
                JobDefinition(
                    "subway",
                    "전철",
                    AdminPermission.SUBWAY,
                    "subway-realtime-cron-job",
                    Duration.ofMinutes(10),
                    "/subway/realtime",
                    true,
                ),
                JobDefinition("cafeteria", "학식", AdminPermission.CAFETERIA, "cafeteria-cron-job", Duration.ofHours(2), "/cafeteria/menu"),
                JobDefinition(
                    "reading-room",
                    "열람실",
                    AdminPermission.READING_ROOM,
                    "reading-room-cron-job",
                    Duration.ofMinutes(5),
                    "/readingRoom/room",
                ),
            )
    }
}
