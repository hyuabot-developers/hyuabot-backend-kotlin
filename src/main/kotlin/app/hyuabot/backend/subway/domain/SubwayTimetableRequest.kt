package app.hyuabot.backend.subway.domain

import io.swagger.v3.oas.annotations.media.Schema

data class SubwayTimetableRequest(
    @get:Schema(description = "시점 ID", example = "K209")
    val startStationID: String,
    @get:Schema(description = "종점 ID", example = "K271")
    val terminalStationID: String,
    @get:Schema(description = "출발 시간", example = "05:30:00")
    val departureTime: String,
    @get:Schema(description = "평일/주말 구분", example = "weekdays")
    val weekday: String,
    @get:Schema(description = "상행/하행 구분", example = "up")
    val direction: String,
)
