package app.hyuabot.backend.subway.domain

import io.swagger.v3.oas.annotations.media.Schema

data class UpdateSubwayRouteRequest(
    @get:Schema(description = "노선 이름", example = "1호선")
    val name: String,
)
