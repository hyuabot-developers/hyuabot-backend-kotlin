package app.hyuabot.backend.holiday.domain

import io.swagger.v3.oas.annotations.media.Schema

data class PublicHolidayResponse(
    @get:Schema(description = "공휴일 ID", example = "1")
    val seq: Int,
    @get:Schema(description = "공휴일 날짜", example = "2025-10-03")
    val date: String,
    @get:Schema(description = "공휴일 이름", example = "개천절")
    val name: String,
    @get:Schema(description = "음력/양력 여부", example = "solar")
    val calendarType: String,
)
