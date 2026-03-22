package app.hyuabot.backend.subway.domain

data class SubwayTimetableKey(
    val stationID: String,
    val directions: Set<String>,
    val weekdays: Set<String>,
) {
    constructor(
        stationID: String,
        directions: List<String>,
        weekdays: List<String>,
    ) : this(stationID, directions.toSet(), weekdays.toSet())
}
