package app.hyuabot.backend.shuttle.domain

import io.swagger.v3.oas.annotations.media.Schema

data class CreateShuttleRouteStopRequest(
    @get:Schema(description = "셔틀버스 정류장 ID", example = "dormitory_o")
    val stopName: String,
    @get:Schema(description = "셔틀버스 경유 순서", example = "1")
    val order: Int,
    @get:Schema(description = "셔틀버스 소요 시간", example = "00:05:00")
    val cumulativeTime: String,
)
