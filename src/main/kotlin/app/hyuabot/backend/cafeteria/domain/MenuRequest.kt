package app.hyuabot.backend.cafeteria.domain

data class MenuRequest(
    val date: String,
    val type: String,
    val food: String,
    val price: String,
)
