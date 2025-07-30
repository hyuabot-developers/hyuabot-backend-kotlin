package app.hyuabot.backend.menu.domain

data class MenuRequest(
    val cafeteriaID: Int,
    val date: String,
    val type: String,
    val food: String,
    val price: String,
)
