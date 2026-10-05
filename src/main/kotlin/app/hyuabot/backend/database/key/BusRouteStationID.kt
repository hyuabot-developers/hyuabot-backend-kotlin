package app.hyuabot.backend.database.key

import java.io.Serializable

data class BusRouteStationID(
    val routeID: Int = 0,
    val stationSeq: Int = 0,
) : Serializable
