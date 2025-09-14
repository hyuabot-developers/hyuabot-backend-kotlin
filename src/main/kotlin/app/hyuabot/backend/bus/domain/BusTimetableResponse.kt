package app.hyuabot.backend.bus.domain

import io.swagger.v3.oas.annotations.media.Schema

data class BusTimetableResponse(
    @get:Schema(description = "버스 시간표 ID", example = "1")
    val seq: Int,
    @get:Schema(description = "노선 ID", example = "100100118")
    val routeID: Int,
    @get:Schema(description = "출발 정류장 ID", example = "1234567")
    val startStopID: Int,
    @get:Schema(description = "평일/토요일/일요일 구분", example = "weekdays")
    val dayType: String,
    @get:Schema(description = "출발 시간 (HH:mm:ss)", example = "08:30:00")
    val departureTime: String,
)
