package app.hyuabot.backend.presence

import app.hyuabot.backend.codegen.types.ShuttleLimitInput
import app.hyuabot.backend.holiday.service.PublicHolidayService
import app.hyuabot.backend.shuttle.domain.ShuttleTimetableKey
import app.hyuabot.backend.shuttle.service.ShuttleHolidayService
import app.hyuabot.backend.shuttle.service.ShuttlePeriodService
import app.hyuabot.backend.shuttle.service.ShuttleTimetableService
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicReference

@Service
class ShuttleDemandWindowService(
    private val shuttleTimetableService: ShuttleTimetableService,
    private val shuttleHolidayService: ShuttleHolidayService,
    private val publicHolidayService: PublicHolidayService,
    private val shuttlePeriodService: ShuttlePeriodService,
) {
    private val cache = AtomicReference<Pair<LocalDate, Map<String, List<LocalTime>>>?>(null)

    data class DemandWindow(
        val startEpoch: Long,
        val keyTtlSeconds: Long,
    )

    fun demandWindow(
        stopName: String,
        now: Instant,
    ): DemandWindow? {
        val zoned = now.atZone(LocalDateTimeBuilder.serviceTimezone)
        val today = zoned.toLocalDate()
        val nowTime = zoned.toLocalTime()

        val times = scheduleFor(today)[stopName] ?: return null
        val next = times.firstOrNull { it > nowTime } ?: return null
        val previous = times.lastOrNull { it <= nowTime }
        val startTime = previous ?: LocalTime.MIDNIGHT

        val startEpoch = ZonedDateTime.of(today, startTime, LocalDateTimeBuilder.serviceTimezone).toEpochSecond()
        val nextEpoch = ZonedDateTime.of(today, next, LocalDateTimeBuilder.serviceTimezone).toEpochSecond()
        return DemandWindow(
            startEpoch = startEpoch,
            keyTtlSeconds = (nextEpoch - now.epochSecond) + KEY_TTL_BUFFER_SECONDS,
        )
    }

    private fun scheduleFor(date: LocalDate): Map<String, List<LocalTime>> {
        val cached = cache.get()
        if (cached != null && cached.first == date) {
            return cached.second
        }
        val computed = computeSchedule(date)
        cache.set(date to computed)
        return computed
    }

    private fun computeSchedule(date: LocalDate): Map<String, List<LocalTime>> {
        val period = shuttlePeriodService.findShuttlePeriod(date) ?: return emptyMap()
        val holiday = shuttleHolidayService.findShuttleHoliday(date)
        val weekdays: List<Boolean> =
            when {
                holiday != null -> if (holiday.type == "weekends") listOf(false) else return emptyMap()
                publicHolidayService.findPublicHoliday(date) != null -> listOf(false)
                else -> listOf(date.dayOfWeek.value != 6 && date.dayOfWeek.value != 7)
            }
        val keys =
            STOP_NAMES
                .map { stop ->
                    ShuttleTimetableKey(
                        stop = stop,
                        periods = setOf(period.type),
                        weekdays = weekdays.toSet(),
                        routes = null,
                        tags = null,
                        destinations = null,
                        after = null,
                        limit = ShuttleLimitInput(order = null, destination = null),
                    )
                }.toSet()
        val results = shuttleTimetableService.getShuttleTimetableBatch(keys)
        return keys.associate { key ->
            key.stop to (results[key]?.order?.map { it.time }?.sorted() ?: emptyList())
        }
    }

    companion object {
        internal val STOP_NAMES =
            listOf(
                "dormitory_o",
                "shuttlecock_o",
                "station",
                "terminal",
                "jungang_stn",
                "shuttlecock_i",
            )
        private const val KEY_TTL_BUFFER_SECONDS = 120L
    }
}
