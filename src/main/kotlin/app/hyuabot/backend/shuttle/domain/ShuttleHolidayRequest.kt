package app.hyuabot.backend.shuttle.domain

import io.swagger.v3.oas.annotations.media.Schema

data class ShuttleHolidayRequest(
    @get:Schema(description = "셔틀버스 휴일 날짜", example = "2025-10-03")
    val date: String,
    @get:Schema(description = "음력/양력 여부", example = "solar")
    val calendarType: String,
    @get:Schema(description = "공휴일/운행 중지 구분", example = "halt")
    val type: String,
)
