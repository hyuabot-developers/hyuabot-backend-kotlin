package app.hyuabot.backend.database.key

import java.io.Serializable
import java.time.ZonedDateTime

data class ShuttlePeriodID(
    val type: String = "",
    val start: ZonedDateTime = ZonedDateTime.now(),
    val end: ZonedDateTime = ZonedDateTime.now(),
) : Serializable
