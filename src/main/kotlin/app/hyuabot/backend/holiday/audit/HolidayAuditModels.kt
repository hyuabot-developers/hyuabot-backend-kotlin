package app.hyuabot.backend.holiday.audit

import java.time.LocalDate
import java.time.ZonedDateTime

data class HolidayAuditIssue(
    val code: String,
    val service: String,
    val date: LocalDate?,
    val message: String,
    val severity: String,
    val managementPath: String,
)

data class HolidayAuditResult(
    val checkedAt: ZonedDateTime,
    val lastSuccessAt: ZonedDateTime?,
    val issues: List<HolidayAuditIssue>,
)
