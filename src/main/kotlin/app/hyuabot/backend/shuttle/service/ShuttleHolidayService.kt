package app.hyuabot.backend.shuttle.service

import app.hyuabot.backend.database.entity.ShuttleHoliday
import app.hyuabot.backend.database.exception.LocalDateNotValidException
import app.hyuabot.backend.database.repository.ShuttleHolidayRepository
import app.hyuabot.backend.shuttle.domain.ShuttleHolidayRequest
import app.hyuabot.backend.shuttle.exception.DuplicateShuttleHolidayException
import app.hyuabot.backend.shuttle.exception.ShuttleHolidayNotFoundException
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ShuttleHolidayService(
    private val shuttleHolidayRepository: ShuttleHolidayRepository,
) {
    fun getShuttleHolidayList() = shuttleHolidayRepository.findAll().sortedBy { it.date }

    fun createShuttleHoliday(payload: ShuttleHolidayRequest): ShuttleHoliday {
        if (!LocalDateTimeBuilder.checkLocalDateFormat(payload.date)) {
            throw LocalDateNotValidException()
        }
        shuttleHolidayRepository
            .findByDateAndCalendarType(
                date = LocalDate.parse(payload.date),
                calendarType = payload.calendarType,
            )?.let {
                throw DuplicateShuttleHolidayException()
            }
        return shuttleHolidayRepository.save(
            ShuttleHoliday(
                date = LocalDate.parse(payload.date),
                calendarType = payload.calendarType,
                type = payload.type,
            ),
        )
    }

    fun getShuttleHolidayById(seq: Int): ShuttleHoliday =
        shuttleHolidayRepository.findById(seq).orElseThrow { throw ShuttleHolidayNotFoundException() }

    fun updateShuttleHoliday(
        seq: Int,
        payload: ShuttleHolidayRequest,
    ): ShuttleHoliday {
        if (!LocalDateTimeBuilder.checkLocalDateFormat(payload.date)) {
            throw LocalDateNotValidException()
        }
        val existingHoliday = shuttleHolidayRepository.findById(seq).orElseThrow { throw ShuttleHolidayNotFoundException() }
        shuttleHolidayRepository
            .findBySeqNotAndDateAndCalendarType(
                seq = seq,
                date = LocalDate.parse(payload.date),
                calendarType = payload.calendarType,
            )?.let {
                throw DuplicateShuttleHolidayException()
            }
        return shuttleHolidayRepository.save(
            existingHoliday.apply {
                date = LocalDate.parse(payload.date)
                calendarType = payload.calendarType
                type = payload.type
            },
        )
    }

    fun deleteShuttleHoliday(seq: Int) {
        val existingHoliday = shuttleHolidayRepository.findById(seq).orElseThrow { throw ShuttleHolidayNotFoundException() }
        shuttleHolidayRepository.delete(existingHoliday)
    }
}
