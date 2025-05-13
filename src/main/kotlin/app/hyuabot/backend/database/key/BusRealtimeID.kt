package app.hyuabot.backend.database.key

import java.io.Serializable

data class BusRealtimeID(
    val routeID: Int,
    val stopID: Int,
    val order: Int,
) : Serializable
