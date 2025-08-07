package app.hyuabot.backend.shuttle.domain

import java.time.ZonedDateTime

data class ShuttlePeriodRequest(
    val type: String,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
)
