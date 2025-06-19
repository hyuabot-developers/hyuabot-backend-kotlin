package app.hyuabot.backend.database.key

import java.io.Serializable

data class CommuteShuttleTimetableID(
    val routeName: String = "",
    val stopName: String = "",
) : Serializable
