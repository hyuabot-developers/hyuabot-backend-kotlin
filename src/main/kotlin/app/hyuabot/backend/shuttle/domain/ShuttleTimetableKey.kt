package app.hyuabot.backend.shuttle.domain

import app.hyuabot.backend.codegen.types.ShuttleLimitInput
import java.time.LocalTime

data class ShuttleTimetableKey(
    val stop: String,
    val periods: Set<String>,
    val weekdays: Set<Boolean>,
    val after: LocalTime?,
    val limit: ShuttleLimitInput,
)
