package app.hyuabot.backend.holiday.audit

data class BusHolidayCoverageGap(
    val routeID: Int,
    val startStopID: Int,
)

data class SubwayHolidayCoverageGap(
    val stationID: String,
    val heading: String,
)
