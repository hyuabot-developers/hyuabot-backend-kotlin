package app.hyuabot.backend.database.key

import java.io.Serializable

data class ShuttleRouteStopID(
    val routeName: String,
    val stopName: String,
) : Serializable
