package app.hyuabot.backend.utility

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class LocalDateTimeBuilder {
    companion object {
        val serviceTimezone = ZoneId.of("Asia/Seoul")
        val datetimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        fun convertAsServiceTimezone(dateTime: ZonedDateTime): LocalDateTime =
            dateTime.withZoneSameInstant(serviceTimezone).toLocalDateTime()

        fun convertLocalDateTimeToString(dateTime: LocalDateTime): String = dateTime.atZone(serviceTimezone).format(datetimeFormatter)
    }
}
