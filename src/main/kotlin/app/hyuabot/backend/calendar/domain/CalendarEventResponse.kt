package app.hyuabot.backend.calendar.domain

import io.swagger.v3.oas.annotations.media.Schema

data class CalendarEventResponse(
    @get:Schema(description = "학사일정 ID", example = "1")
    val seq: Int,
    @get:Schema(description = "학사일정 제목", example = "중간고사")
    val title: String,
    @get:Schema(description = "학사일정 내용", example = "중간고사 기간입니다.")
    val description: String,
    @get:Schema(description = "학사일정 시작 날짜", example = "2024-10-14")
    val start: String,
    @get:Schema(description = "학사일정 종료 날짜", example = "2024-10-20")
    val end: String,
    @get:Schema(description = "학사일정 카테고리 ID", example = "1")
    val categoryID: Int,
)
