package app.hyuabot.backend.shuttle.service

import app.hyuabot.backend.database.entity.ShuttlePeriod
import app.hyuabot.backend.database.exception.LocalDateTimeNotValidException
import app.hyuabot.backend.database.repository.ShuttlePeriodRepository
import app.hyuabot.backend.shuttle.domain.ShuttlePeriodRequest
import app.hyuabot.backend.shuttle.exception.ShuttlePeriodNotFoundException
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class ShuttlePeriodService(
    private val shuttlePeriodRepository: ShuttlePeriodRepository,
) {
    fun getShuttlePeriodList() = shuttlePeriodRepository.findAll().sortedBy { it.start }

    fun createShuttlePeriod(payload: ShuttlePeriodRequest): ShuttlePeriod {
        if (payload.start >= payload.end) {
            throw IllegalArgumentException("Start time must be before end time")
        } else if (
            !LocalDateTimeBuilder.checkLocalDateTimeFormat(payload.start) ||
            !LocalDateTimeBuilder.checkLocalDateTimeFormat(payload.end)
        ) {
            throw LocalDateTimeNotValidException()
        }

        return shuttlePeriodRepository.save(
            ShuttlePeriod(
                start =
                    payload.start.let {
                        ZonedDateTime.of(
                            LocalDateTime.parse(it, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            LocalDateTimeBuilder.serviceTimezone,
                        )
                    },
                end =
                    payload.end.let {
                        ZonedDateTime.of(
                            LocalDateTime.parse(it, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            LocalDateTimeBuilder.serviceTimezone,
                        )
                    },
                type = payload.type,
                periodType = null,
            ),
        )
    }

    fun getShuttlePeriod(seq: Int): ShuttlePeriod =
        shuttlePeriodRepository.findById(seq).orElseThrow {
            throw ShuttlePeriodNotFoundException()
        }

    fun updateShuttlePeriod(
        seq: Int,
        payload: ShuttlePeriodRequest,
    ): ShuttlePeriod {
        if (
            !LocalDateTimeBuilder.checkLocalDateTimeFormat(payload.start) ||
            !LocalDateTimeBuilder.checkLocalDateTimeFormat(payload.end)
        ) {
            throw LocalDateTimeNotValidException()
        }

        return shuttlePeriodRepository
            .findById(seq)
            .orElseThrow {
                ShuttlePeriodNotFoundException()
            }.let { shuttlePeriod ->
                shuttlePeriod.apply {
                    start =
                        payload.start.let {
                            ZonedDateTime.of(
                                LocalDateTime.parse(it, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                                LocalDateTimeBuilder.serviceTimezone,
                            )
                        }
                    end =
                        payload.end.let {
                            ZonedDateTime.of(
                                LocalDateTime.parse(it, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                                LocalDateTimeBuilder.serviceTimezone,
                            )
                        }
                    type = payload.type
                }
                shuttlePeriodRepository.save(shuttlePeriod)
            }
    }

    fun deleteShuttlePeriod(seq: Int) {
        shuttlePeriodRepository
            .findById(seq)
            .orElseThrow {
                throw ShuttlePeriodNotFoundException()
            }.let {
                shuttlePeriodRepository.delete(it)
            }
    }
}
