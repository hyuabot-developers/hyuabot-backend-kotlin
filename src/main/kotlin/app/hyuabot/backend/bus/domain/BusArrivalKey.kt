package app.hyuabot.backend.bus.domain

data class BusArrivalKey(
    val routeID: Int,
    val stopID: Int,
    val limit: Int?,
)
