package app.hyuabot.backend.database.key

import java.io.Serializable

data class BusRouteStopID(
    val routeID: Int,
    val stopID: Int,
) : Serializable
