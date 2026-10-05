package app.hyuabot.backend.database.key

import java.io.Serializable

data class BusLocationID(
    val routeID: Int = 0,
    val plateNumber: String = "",
) : Serializable
