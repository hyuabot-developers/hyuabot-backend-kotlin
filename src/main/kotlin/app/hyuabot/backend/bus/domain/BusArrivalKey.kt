package app.hyuabot.backend.bus.domain

data class BusArrivalKey(
    val routeID: Int,
    val stopID: Int,
    val startStopID: Int,
    val minuteFromStart: Int,
    val limit: Int?,
    val destinationStopID: Int? = null,
    val destinationStopIDs: Set<Int> = emptySet(),
)
