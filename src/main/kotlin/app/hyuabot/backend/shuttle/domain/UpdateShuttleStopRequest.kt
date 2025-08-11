package app.hyuabot.backend.shuttle.domain

import io.swagger.v3.oas.annotations.media.Schema

data class UpdateShuttleStopRequest(
    @get:Schema(description = "셔틀버스 정류장 위도", example = "37.123456")
    val latitude: Double,
    @get:Schema(description = "셔틀버스 정류장 경도", example = "127.123456")
    val longitude: Double,
)
