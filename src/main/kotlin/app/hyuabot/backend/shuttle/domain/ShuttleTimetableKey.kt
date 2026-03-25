package app.hyuabot.backend.shuttle.domain

import java.time.LocalTime

data class ShuttleTimetableKey(
    val stop: String,
    val periods: Set<String>,
    val weekdays: Set<Boolean>,
    val after: LocalTime?,
    val limit: Int?,
)
