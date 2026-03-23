package app.hyuabot.backend.shuttle.domain

import java.time.LocalTime

data class ShuttleTimetableKey(
    val stop: String,
    val periods: Set<String>,
    val weekdays: Set<Boolean>,
    val after: LocalTime? = null,
    val limit: Int? = null,
) {
    constructor(
        stop: String,
        periods: List<String>,
        weekdays: List<Boolean>,
        after: LocalTime? = null,
        limit: Int? = null,
    ) : this(
        stop = stop,
        periods = periods.toSet(),
        weekdays = weekdays.toSet(),
        after = after,
        limit = limit,
    )
}
