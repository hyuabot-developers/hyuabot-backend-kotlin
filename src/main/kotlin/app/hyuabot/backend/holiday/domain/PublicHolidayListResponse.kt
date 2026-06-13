package app.hyuabot.backend.holiday.domain

import io.swagger.v3.oas.annotations.media.Schema

data class PublicHolidayListResponse(
    @get:Schema(description = "공휴일 목록")
    val result: List<PublicHolidayResponse>,
)
