package app.hyuabot.backend.shuttle.domain

import io.swagger.v3.oas.annotations.media.Schema

data class ShuttlePeriodResponse(
    @get:Schema(description = "셔틀버스 운행 기간 ID", example = "1")
    val seq: Int,
    @get:Schema(description = "셔틀버스 운행 기간 종류", example = "semester")
    val type: String,
    @get:Schema(description = "셔틀버스 운행 기간 시작", example = "2025-09-01 00:00:00")
    val start: String,
    @get:Schema(description = "셔틀버스 운행 기간 종료", example = "2025-12-31 23:59:59")
    val end: String,
)
