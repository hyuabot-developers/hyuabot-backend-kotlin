package app.hyuabot.backend.database.key

import java.io.Serializable

data class BusRealtimeID(
    val routeID: Int = 0,
    val stopID: Int = 0,
    val order: Int = 0,
) : Serializable
