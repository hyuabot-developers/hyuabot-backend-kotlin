package app.hyuabot.backend.bus.domain

import io.swagger.v3.oas.annotations.media.Schema

data class BusRealtimeResponse(
    @get:Schema(description = "버스 노선 ID", example = "1234567")
    val routeID: Int,
    @get:Schema(description = "버스 정류장 ID", example = "7654321")
    val stopID: Int,
    @get:Schema(description = "버스 도착 순서", example = "1")
    val order: Int,
    @get:Schema(description = "남은 정류장 수", example = "3")
    val stop: Int,
    @get:Schema(description = "남은 시간(분)", example = "00:05:00")
    val time: String,
    @get:Schema(description = "남은 좌석 수", example = "10")
    val seat: Int,
    @get:Schema(description = "저상 버스 여부", example = "true")
    val isLowFloor: Boolean,
    @get:Schema(description = "도착 정보 갱신 시간", example = "2023-10-01 12:34:56")
    val updatedAt: String,
)
