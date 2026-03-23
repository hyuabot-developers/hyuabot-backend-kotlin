package app.hyuabot.backend.shuttle.domain

import java.time.LocalTime

data class ShuttleArrivalItem(
    val stop: String,
    val time: LocalTime,
)
