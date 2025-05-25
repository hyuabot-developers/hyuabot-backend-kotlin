package app.hyuabot.backend.database.key

import java.io.Serializable
import java.time.LocalDate

data class MenuID(
    val restaurantID: Int,
    val date: LocalDate,
    val type: String,
    val food: String,
    val price: String,
) : Serializable
