package app.hyuabot.backend.database.entity

import java.time.LocalDate
import java.time.ZonedDateTime

data class HolidaySyncState(
    val source: String,
    val lastAttemptAt: ZonedDateTime?,
    val lastSuccessAt: ZonedDateTime?,
    val rangeStart: LocalDate?,
    val rangeEnd: LocalDate?,
    val lastError: String?,
)
