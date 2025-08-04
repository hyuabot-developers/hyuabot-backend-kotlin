package app.hyuabot.backend.cafeteria.domain

data class CafeteriaResponse(
    val seq: Int,
    val campusID: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val breakfastTime: String?,
    val lunchTime: String?,
    val dinnerTime: String?,
)
