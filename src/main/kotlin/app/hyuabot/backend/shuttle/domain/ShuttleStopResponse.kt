package app.hyuabot.backend.shuttle.domain

import io.swagger.v3.oas.annotations.media.Schema

data class ShuttleStopResponse(
    @get:Schema(description = "셔틀버스 정류장 ID", example = "dormitory_o")
    val name: String,
    @get:Schema(description = "셔틀버스 정류장 위도", example = "37.5665")
    val latitude: Double,
    @get:Schema(description = "셔틀버스 정류장 경도", example = "126.9789")
    val longitude: Double,
)
