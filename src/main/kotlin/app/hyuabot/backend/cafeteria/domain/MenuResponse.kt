package app.hyuabot.backend.cafeteria.domain

import java.time.LocalDate

data class MenuResponse(
    val seq: Int,
    val cafeteriaID: Int,
    val date: LocalDate,
    val type: String,
    val food: String,
    val price: String,
)
