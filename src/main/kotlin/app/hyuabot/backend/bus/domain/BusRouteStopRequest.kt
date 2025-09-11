package app.hyuabot.backend.bus.domain

import io.swagger.v3.oas.annotations.media.Schema

data class BusRouteStopRequest(
    @get:Schema(description = "정류장 ID", example = "1234567")
    val stopID: Int,
    @get:Schema(description = "경유 순서", example = "1")
    val order: Int,
    @get:Schema(description = "출발 정류장 ID", example = "1234567")
    val startStopID: Int,
    @get:Schema(description = "출방 정류장부터 소요 사건(분)", example = "5")
    val travelTime: Int,
)
