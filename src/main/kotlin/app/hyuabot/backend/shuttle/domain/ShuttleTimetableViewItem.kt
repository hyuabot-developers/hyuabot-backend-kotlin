package app.hyuabot.backend.shuttle.domain

import java.time.LocalTime

data class ShuttleTimetableViewItem(
    val seq: Int,
    val routeName: String,
    val routeTag: String,
    val period: String,
    val weekday: Boolean,
    val time: LocalTime,
    val group: String?,
    val stops: List<ShuttleArrivalItem>,
)
