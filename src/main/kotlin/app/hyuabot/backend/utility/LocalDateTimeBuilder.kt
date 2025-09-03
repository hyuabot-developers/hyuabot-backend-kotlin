package app.hyuabot.backend.utility

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object LocalDateTimeBuilder {
    val serviceTimezone: ZoneId = ZoneId.of("Asia/Seoul")
    val datetimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun convertAsServiceTimezone(dateTime: ZonedDateTime): LocalDateTime = dateTime.withZoneSameInstant(serviceTimezone).toLocalDateTime()

    fun convertLocalTimeToString(time: LocalTime): String = time.format(timeFormatter)

    fun convertLocalDateTimeToString(dateTime: LocalDateTime): String = dateTime.atZone(serviceTimezone).format(datetimeFormatter)

    fun convertDurationToString(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutesPart()
        val seconds = duration.toSecondsPart()
        return if (duration.isNegative) {
            "-${String.format("%02d:%02d:%02d", -hours, -minutes, -seconds)}"
        } else {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    fun convertStringToDuration(duration: String): Duration {
        val parts = duration.split(":").map { it.toInt() }
        return Duration.ofHours(parts[0].toLong()).plusMinutes(parts[1].toLong()).plusSeconds(parts[2].toLong())
    }

    fun checkLocalDateRange(
        start: String,
        end: String,
    ): Boolean {
        if (!checkLocalDateFormat(start) || !checkLocalDateFormat(end)) {
            return false
        }
        val startDate = LocalDate.parse(start, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val endDate = LocalDate.parse(end, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return !startDate.isAfter(endDate)
    }

    fun checkLocalDateFormat(date: String): Boolean {
        if (!date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            return false
        }
        return try {
            LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun checkLocalDateTimeFormat(dateTime: String): Boolean {
        if (!dateTime.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"))) {
            return false
        }
        return try {
            LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            true
        } catch (e: Exception) {
            false
        }
    }
}
