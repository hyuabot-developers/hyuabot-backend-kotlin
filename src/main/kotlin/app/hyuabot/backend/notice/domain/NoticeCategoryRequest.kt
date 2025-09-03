package app.hyuabot.backend.notice.domain

import io.swagger.v3.oas.annotations.media.Schema

data class NoticeCategoryRequest(
    @get:Schema(description = "공지 카테고리 이름", example = "학사공지") val name: String,
)
