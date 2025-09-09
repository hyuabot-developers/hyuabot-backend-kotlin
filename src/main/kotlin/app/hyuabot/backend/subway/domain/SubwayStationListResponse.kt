package app.hyuabot.backend.subway.domain

import io.swagger.v3.oas.annotations.media.Schema

data class SubwayStationListResponse(
    @get:Schema(description = "지하철 역 목록")
    val result: List<SubwayStationResponse>,
)
