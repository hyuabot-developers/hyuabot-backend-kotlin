package app.hyuabot.backend.subway.domain

import io.swagger.v3.oas.annotations.media.Schema

data class CreateSubwayStationRequest(
    @get:Schema(description = "역 ID", example = "K251")
    val id: String,
    @get:Schema(description = "역 이름", example = "서울역")
    val name: String,
    @get:Schema(description = "노선 ID", example = "1001")
    val routeID: Int,
    @get:Schema(description = "역 순서", example = "1")
    val order: Int,
    @get:Schema(description = "종점으로부터의 시간", example = "00:05:00")
    val cumulativeTime: String,
)
