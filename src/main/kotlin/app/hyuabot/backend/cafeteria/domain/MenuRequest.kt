package app.hyuabot.backend.cafeteria.domain

import java.time.LocalDate

data class MenuRequest(
    val date: LocalDate,
    val type: String,
    val food: String,
    val price: String,
)
