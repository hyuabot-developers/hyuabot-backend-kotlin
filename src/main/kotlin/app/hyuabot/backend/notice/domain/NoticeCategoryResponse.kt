package app.hyuabot.backend.notice.domain

import io.swagger.v3.oas.annotations.media.Schema

data class NoticeCategoryResponse(
    @get:Schema(description = "공지사항 카테고리 ID", example = "1")
    val seq: Int,
    @get:Schema(description = "공지사항 카테고리 이름", example = "학사공지")
    val name: String,
)
