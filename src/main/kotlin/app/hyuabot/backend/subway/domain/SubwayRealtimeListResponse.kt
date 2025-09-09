package app.hyuabot.backend.subway.domain

import io.swagger.v3.oas.annotations.media.Schema

data class SubwayRealtimeListResponse(
    @get:Schema(description = "지하철 실시간 도착 정보 목록")
    val result: List<SubwayRealtimeResponse>,
)
