package app.hyuabot.backend.shuttle.domain

import io.swagger.v3.oas.annotations.media.Schema

data class ShuttleTimetableResponse(
    @get:Schema(description = "셔틀버스 시간표 ID", example = "1")
    val seq: Int,
    @get:Schema(description = "셔틀버스 운행 기간 종류", example = "semester")
    val period: String,
    @get:Schema(description = "셔틀버스 시간표 평일/주말 구분", example = "true")
    val isWeekdays: Boolean,
    @get:Schema(description = "셔틀버스 출발 시간", example = "08:00:00")
    val departureTime: String,
)
