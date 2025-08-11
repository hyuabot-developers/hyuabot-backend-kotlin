package app.hyuabot.backend.shuttle.domain

import io.swagger.v3.oas.annotations.media.Schema

data class ShuttleRouteStopResponse(
    @get:Schema(description = "셔틀버스 노선-정류장 ID", example = "1")
    val seq: Int,
    @get:Schema(description = "셔틀버스 정류장 ID", example = "dormitory_o")
    val name: String,
    @get:Schema(description = "셔틀버스 경유 순서", example = "1")
    val order: Int,
    @get:Schema(description = "셔틀버스 종점발 소요 시간", example = "10:00:00")
    val cumulativeTime: String,
)
