package app.hyuabot.backend.calendar.domain

import io.swagger.v3.oas.annotations.media.Schema

data class CalendarCategoryResponse(
    @get:Schema(description = "학사일정 카테고리 ID", example = "1")
    val seq: Int,
    @get:Schema(description = "학사일정 카테고리 이름", example = "중요")
    val name: String,
)
