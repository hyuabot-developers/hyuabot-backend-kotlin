package app.hyuabot.backend.calendar.domain

import io.swagger.v3.oas.annotations.media.Schema

data class CalendarCategoryRequest(
    @get:Schema(description = "캘린더 카테고리 이름", example = "학사일정")
    val name: String,
)
