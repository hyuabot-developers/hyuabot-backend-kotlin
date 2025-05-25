package app.hyuabot.backend.database.key

import java.io.Serializable

data class RoomID(
    val buildingName: String,
    val number: String,
) : Serializable
