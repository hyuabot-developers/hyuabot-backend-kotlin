package app.hyuabot.backend.bus.domain

import java.time.LocalDate

data class BusDepartureLogKey(
    val routeID: Int,
    val stopID: Int,
    val dates: List<LocalDate>,
    val limit: Int? = null,
)
