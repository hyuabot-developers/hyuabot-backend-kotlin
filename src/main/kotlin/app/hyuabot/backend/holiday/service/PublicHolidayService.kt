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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class PublicHolidayService(
    private val publicHolidayRepository: PublicHolidayRepository,
) {
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
        publicHolidayRepository.delete(existing)
    }

    fun findPublicHoliday(date: LocalDate): PublicHoliday? {
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
    }
}
