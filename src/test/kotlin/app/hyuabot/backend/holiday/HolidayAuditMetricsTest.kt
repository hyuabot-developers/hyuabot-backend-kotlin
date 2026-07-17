package app.hyuabot.backend.holiday

import app.hyuabot.backend.holiday.audit.HolidayAuditIssue
import app.hyuabot.backend.holiday.audit.HolidayAuditMetrics
import app.hyuabot.backend.holiday.audit.HolidayAuditResult
import app.hyuabot.backend.holiday.audit.HolidayAuditService
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class HolidayAuditMetricsTest {
    @Test
    fun `gauges publish issue counts and synchronized age from a cached audit`() {
        val service = mock<HolidayAuditService>()
        val registry = SimpleMeterRegistry()
        val checkedAt = ZonedDateTime.of(2026, 7, 17, 9, 0, 0, 0, ZoneId.of("Asia/Seoul"))
        whenever(service.audit(any(), any())).thenReturn(
            HolidayAuditResult(
                checkedAt,
                checkedAt.minusHours(2),
                listOf(
                    issue("WARNING", checkedAt.toLocalDate().plusDays(2)),
                    issue("ERROR", checkedAt.toLocalDate()),
                    issue("ERROR", checkedAt.toLocalDate().plusDays(1)),
                ),
            ),
        )
        val metrics = HolidayAuditMetrics(service, registry)
        val firstRead = Instant.now()
        metrics.snapshot(firstRead)

        assertEquals(
            1.0,
            registry
                .get("hyuabot.holiday.configuration.issues")
                .tag("severity", "warning")
                .gauge()
                .value(),
        )
        assertEquals(
            2.0,
            registry
                .get("hyuabot.holiday.configuration.issues")
                .tag("severity", "error")
                .gauge()
                .value(),
        )
        assertEquals(
            1.0,
            registry
                .get("hyuabot.holiday.configuration.due.issues")
                .tag("window", "today")
                .gauge()
                .value(),
        )
        assertEquals(
            1.0,
            registry
                .get("hyuabot.holiday.configuration.due.issues")
                .tag("window", "tomorrow")
                .gauge()
                .value(),
        )
        assertEquals(7200.0, registry.get("hyuabot.holiday.sync.age.seconds").gauge().value())
        metrics.snapshot(firstRead.plusSeconds(4 * 60))
        metrics.snapshot(firstRead.plusSeconds(6 * 60))

        verify(service, times(2)).audit(any(), any())
    }

    @Test
    fun `missing successful sync is exported as negative age`() {
        val service = mock<HolidayAuditService>()
        val registry = SimpleMeterRegistry()
        whenever(service.audit(any(), any())).thenReturn(
            HolidayAuditResult(ZonedDateTime.now(ZoneId.of("Asia/Seoul")), null, emptyList()),
        )

        HolidayAuditMetrics(service, registry)

        assertEquals(-1.0, registry.get("hyuabot.holiday.sync.age.seconds").gauge().value())
    }

    private fun issue(
        severity: String,
        date: LocalDate,
    ) = HolidayAuditIssue(
        code = "TEST",
        service = "holiday",
        date = date,
        message = "test",
        severity = severity,
        managementPath = "/holiday",
    )
}
