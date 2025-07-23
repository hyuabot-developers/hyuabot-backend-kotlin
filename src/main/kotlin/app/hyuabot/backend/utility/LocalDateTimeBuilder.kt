package app.hyuabot.backend.utility

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object LocalDateTimeBuilder {
    private val serviceTimezone: ZoneId = ZoneId.of("Asia/Seoul")
    private val datetimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun convertAsServiceTimezone(dateTime: ZonedDateTime): LocalDateTime = dateTime.withZoneSameInstant(serviceTimezone).toLocalDateTime()

    fun convertLocalDateTimeToString(dateTime: LocalDateTime): String = dateTime.atZone(serviceTimezone).format(datetimeFormatter)
}
