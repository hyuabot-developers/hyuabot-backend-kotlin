package app.hyuabot.backend.bus.domain

import io.swagger.v3.oas.annotations.media.Schema

data class BusDepartureLogResponse(
    @get:Schema(description = "버스 노선 ID", example = "1")
    val routeID: Int,
    @get:Schema(description = "버스 정류장 ID", example = "10010000101")
    val stopID: Int,
    @get:Schema(description = "로그 기록된 날짜", example = "2023-10-01")
    val date: String,
    @get:Schema(description = "로그 기록된 시간", example = "12:34:56")
    val time: String,
    @get:Schema(description = "출발 차량 ID", example = "123456789")
    val vehicleID: String,
)
