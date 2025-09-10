package app.hyuabot.backend.subway.domain

import io.swagger.v3.oas.annotations.media.Schema

data class SubwayTimetableListResponse(
    @get:Schema(description = "지하철 시간표 목록")
    val result: List<SubwayTimetableResponse>,
)
