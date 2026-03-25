package app.hyuabot.backend.shuttle.domain

import java.time.LocalDate
import java.time.LocalTime

data class ShuttleFilterContext(
    val stops: List<String>?,
    val periods: List<String>,
    val weekdays: List<Boolean>,
    val date: LocalDate?,
    val after: LocalTime?,
    val limit: Int?,
    val isHalt: Boolean,
)
