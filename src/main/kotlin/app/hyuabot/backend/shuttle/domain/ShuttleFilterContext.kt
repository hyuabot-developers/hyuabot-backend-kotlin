package app.hyuabot.backend.shuttle.domain

import app.hyuabot.backend.codegen.types.ShuttleStopInput
import java.time.LocalDate
import java.time.LocalTime

data class ShuttleFilterContext(
    val stops: List<ShuttleStopInput>?,
    val periods: List<String>,
    val weekdays: List<Boolean>,
    val date: LocalDate?,
    val after: LocalTime?,
    val isHalt: Boolean,
)
