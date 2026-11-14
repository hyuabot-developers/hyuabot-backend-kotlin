package app.hyuabot.backend.bus.domain

import java.time.LocalDate
import java.time.LocalTime

data class BusDepartureLogKey(
    val routeID: Int,
    val stopID: Int,
    val dates: List<LocalDate>,
    val limit: Int? = null,
    /**
     * Client time of day (BusRouteStopInput.logAfter). When set, only logs at or after it and post-midnight logs
     * (before [POST_MIDNIGHT_END], which the client shows as the next day) are kept, and inside each date the
     * post-midnight ones that are before `after` come last. Null keeps the legacy behavior.
     */
    val after: LocalTime? = null,
) {
    fun includes(departureTime: LocalTime): Boolean =
        after == null || !departureTime.isBefore(after) || departureTime.isBefore(POST_MIDNIGHT_END)

    /** 0 for logs later today (or every log without [after]), 1 for logs that wrap past midnight. */
    fun wrapRank(departureTime: LocalTime): Int = if (after == null || !departureTime.isBefore(after)) 0 else 1

    companion object {
        /** End of the post-midnight bucket; must match `TIME '04:00:00'` in BusDepartureLogRepositoryImpl. */
        val POST_MIDNIGHT_END: LocalTime = LocalTime.of(4, 0)
    }
}
