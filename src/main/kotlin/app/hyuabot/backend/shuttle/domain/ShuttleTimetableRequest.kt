package app.hyuabot.backend.shuttle.domain

import io.swagger.v3.oas.annotations.media.Schema

data class ShuttleTimetableRequest(
    @get:Schema(description = "셔틀버스 운행 주기 종류", example = "semester")
    val periodType: String,
    @get:Schema(description = "셔틀버스 시간표 평일/주말 구분", example = "true")
    val weekday: Boolean,
    @get:Schema(description = "셔틀버스 출발 시간", example = "08:00:00")
    val departureTime: String,
)
