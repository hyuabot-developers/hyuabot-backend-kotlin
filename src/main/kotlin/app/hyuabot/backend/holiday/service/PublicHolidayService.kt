package app.hyuabot.backend.holiday.service

import app.hyuabot.backend.database.entity.PublicHoliday
import app.hyuabot.backend.database.exception.LocalDateNotValidException
import app.hyuabot.backend.database.repository.PublicHolidayRepository
import app.hyuabot.backend.holiday.domain.PublicHolidayRequest
import app.hyuabot.backend.holiday.exception.DuplicatePublicHolidayException
import app.hyuabot.backend.holiday.exception.PublicHolidayNotFoundException
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import com.github.usingsky.calendar.KoreanLunarCalendar
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

@Service
class PublicHolidayService(
    private val publicHolidayRepository: PublicHolidayRepository,
) {
    /**
     * A single GraphQL request resolves the holiday for the same date from the bus, subway (per station) and shuttle
     * fetchers. A short-lived per-date memo removes those repeated queries; admin writes on this instance clear it
     * immediately and other instances pick changes up within [HOLIDAY_CACHE_TTL_NANOS].
     */
    private val holidayCache = ConcurrentHashMap<LocalDate, Pair<Long, PublicHoliday?>>()

    /** Monotonic clock for the cache TTL; replaceable in tests. */
    internal var nanoClock: () -> Long = System::nanoTime

    fun getPublicHolidayList() = publicHolidayRepository.findAll().sortedBy { it.date }

    fun createPublicHoliday(payload: PublicHolidayRequest): PublicHoliday {
        require(payload.calendarType in CALENDAR_TYPES) { "Unsupported calendar type" }
        if (!LocalDateTimeBuilder.checkLocalDateFormat(payload.date)) {
            throw LocalDateNotValidException()
        }
        publicHolidayRepository
            .findByDateAndCalendarType(
                date = LocalDate.parse(payload.date),
                calendarType = payload.calendarType,
            )?.let {
                throw DuplicatePublicHolidayException()
            }
        holidayCache.clear()
        return publicHolidayRepository.save(
            PublicHoliday(
                date = LocalDate.parse(payload.date),
                name = payload.name,
                calendarType = payload.calendarType,
            ),
        )
    }

    fun getPublicHolidayById(seq: Int): PublicHoliday =
        publicHolidayRepository.findById(seq).orElseThrow { throw PublicHolidayNotFoundException() }

    fun updatePublicHoliday(
        seq: Int,
        payload: PublicHolidayRequest,
    ): PublicHoliday {
        require(payload.calendarType in CALENDAR_TYPES) { "Unsupported calendar type" }
        if (!LocalDateTimeBuilder.checkLocalDateFormat(payload.date)) {
            throw LocalDateNotValidException()
        }
        val existing = publicHolidayRepository.findById(seq).orElseThrow { throw PublicHolidayNotFoundException() }
        publicHolidayRepository
            .findBySeqNotAndDateAndCalendarType(
                seq = seq,
                date = LocalDate.parse(payload.date),
                calendarType = payload.calendarType,
            )?.let {
                throw DuplicatePublicHolidayException()
            }
        holidayCache.clear()
        return publicHolidayRepository.save(
            existing.apply {
                date = LocalDate.parse(payload.date)
                name = payload.name
                calendarType = payload.calendarType
            },
        )
    }

    fun deletePublicHoliday(seq: Int) {
        val existing = publicHolidayRepository.findById(seq).orElseThrow { throw PublicHolidayNotFoundException() }
        holidayCache.clear()
        publicHolidayRepository.delete(existing)
    }

    fun findPublicHoliday(date: LocalDate): PublicHoliday? {
        val now = nanoClock()
        holidayCache[date]?.let { (cachedAt, holiday) -> if (now - cachedAt < HOLIDAY_CACHE_TTL_NANOS) return holiday }
        return lookupPublicHoliday(date).also { holidayCache[date] = now to it }
    }

    private fun lookupPublicHoliday(date: LocalDate): PublicHoliday? {
        val lunarDate = KoreanLunarCalendar.getInstance()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        lunarDate.setSolarDate(date.year, date.monthValue, date.dayOfMonth)
        return publicHolidayRepository.findBySolarDateOrLunarDate(
            date,
            LocalDate.parse(lunarDate.lunarIsoFormat, dateFormatter),
        )
    }

    companion object {
        private val CALENDAR_TYPES = setOf("solar", "lunar")
        private val HOLIDAY_CACHE_TTL_NANOS = Duration.ofMinutes(1).toNanos()
    }
}
