package app.hyuabot.backend.database.key

import java.io.Serializable

data class SubwayRealtimeID(
    val stationID: String,
    val heading: String,
    val order: Int,
) : Serializable
