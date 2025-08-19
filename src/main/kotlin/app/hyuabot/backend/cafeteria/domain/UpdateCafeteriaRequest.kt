package app.hyuabot.backend.cafeteria.domain

import io.swagger.v3.oas.annotations.media.Schema

data class UpdateCafeteriaRequest(
    @get:Schema(description = "소속 캠퍼스 ID", example = "1")
    val campusID: Int,
    @get:Schema(description = "식당 이름", example = "제1학생회관 식당")
    val name: String,
    @get:Schema(description = "식당 위도", example = "37.123456")
    val latitude: Double,
    @get:Schema(description = "식당 경도", example = "127.123456")
    val longitude: Double,
    @get:Schema(description = "조식 시간", example = "08:00-09:30")
    val breakfastTime: String,
    @get:Schema(description = "중식 시간", example = "12:00-13:30")
    val lunchTime: String,
    @get:Schema(description = "석식 시간", example = "18:00-19:30")
    val dinnerTime: String,
)
