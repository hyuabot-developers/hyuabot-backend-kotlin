package app.hyuabot.backend.database.key

import java.io.Serializable
import java.time.LocalDate

data class MenuID(
    val restaurantID: Int = 0,
    val date: LocalDate = LocalDate.now(),
    val type: String = "",
    val food: String = "",
    val price: String = "",
) : Serializable
