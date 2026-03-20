package app.hyuabot.backend.subway.domain

data class SubwayTimetableKey(
    val stationID: String,
    val directions: List<String>,
    val weekdays: List<String>,
)
