package app.hyuabot.backend.database.key

import java.io.Serializable

data class BusRouteStopID(
    val routeID: Int = 0,
    val stopID: Int = 0,
) : Serializable
