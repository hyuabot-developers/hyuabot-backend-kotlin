package app.hyuabot.backend.menu.domain

data class MenuResponse(
    val seq: Int,
    val cafeteriaID: Int,
    val date: String,
    val type: String,
    val food: String,
    val price: String,
)
