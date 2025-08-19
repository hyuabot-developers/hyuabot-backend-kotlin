package app.hyuabot.backend.cafeteria.domain

import io.swagger.v3.oas.annotations.media.Schema

data class CafeteriaResponse(
    @get:Schema(description = "식당 ID", example = "1")
    val seq: Int,
    @get:Schema(description = "소속 캠퍼스 ID", example = "1")
    val campusID: Int,
    @get:Schema(description = "식당 이름", example = "제1학생회관 식당")
    val name: String,
    @get:Schema(description = "식당 위도", example = "37.123456")
    val latitude: Double,
    @get:Schema(description = "식당 경도", example = "127.123456")
    val longitude: Double,
    @get:Schema(description = "조식 시간 설명", example = "조식은 08:00부터 09:30까지 제공됩니다.")
    val breakfastTime: String?,
    @get:Schema(description = "중식 시간 설명", example = "중식은 12:00부터 13:30까지 제공됩니다.")
    val lunchTime: String?,
    @get:Schema(description = "석식 시간 설명", example = "석식은 18:00부터 19:30까지 제공됩니다.")
    val dinnerTime: String?,
)
