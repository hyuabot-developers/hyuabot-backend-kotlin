package app.hyuabot.backend.cafeteria.domain

data class UpdateCafeteriaRequest(
    val campusID: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val breakfastTime: String,
    val lunchTime: String,
    val dinnerTime: String,
)
