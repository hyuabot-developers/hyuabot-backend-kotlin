package app.hyuabot.backend.holiday.audit

import app.hyuabot.backend.security.AdminPermission
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class HolidayAuditMetrics(
    private val service: HolidayAuditService,
    meterRegistry: MeterRegistry,
) {
    private var cachedAt = Instant.EPOCH
    private var cachedResult: HolidayAuditResult? = null

    init {
        listOf("WARNING", "ERROR").forEach { severity ->
            Gauge
                .builder("hyuabot.holiday.configuration.issues") {
                    snapshot().issues.count { it.severity == severity }.toDouble()
                }.tag("severity", severity.lowercase())
                .register(meterRegistry)
        }
        listOf("today" to 0L, "tomorrow" to 1L).forEach { (window, daysFromToday) ->
            Gauge
                .builder("hyuabot.holiday.configuration.due.issues") {
                    val audit = snapshot()
                    val targetDate = audit.checkedAt.toLocalDate().plusDays(daysFromToday)
                    audit.issues.count { it.date == targetDate }.toDouble()
                }.tag("window", window)
                .register(meterRegistry)
        }
        Gauge
            .builder("hyuabot.holiday.sync.age.seconds") {
                snapshot().lastSuccessAt?.let { Duration.between(it, snapshot().checkedAt).seconds.toDouble() } ?: -1.0
            }.register(meterRegistry)
    }

    @Synchronized
    internal fun snapshot(now: Instant = Instant.now()): HolidayAuditResult {
        val current = cachedResult
        if (current == null || Duration.between(cachedAt, now) >= CACHE_DURATION) {
            cachedAt = now
            cachedResult = service.audit(AdminPermission.entries.toSet())
        }
        return cachedResult!!
    }

    companion object {
        private val CACHE_DURATION = Duration.ofMinutes(5)
    }
}
