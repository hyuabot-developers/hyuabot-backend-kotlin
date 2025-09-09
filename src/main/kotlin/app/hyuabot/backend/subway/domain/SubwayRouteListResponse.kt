package app.hyuabot.backend.subway.domain

import io.swagger.v3.oas.annotations.media.Schema

data class SubwayRouteListResponse(
    @get:Schema(description = "지하철 노선 목록")
    val result: List<SubwayRouteResponse>,
)
