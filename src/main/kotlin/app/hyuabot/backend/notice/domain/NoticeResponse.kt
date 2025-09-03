package app.hyuabot.backend.notice.domain

import io.swagger.v3.oas.annotations.media.Schema

data class NoticeResponse(
    @get:Schema(description = "공지 ID", example = "1")
    val seq: Int,
    @get:Schema(description = "공지 카테고리 ID", example = "1")
    val categoryID: Int,
    @get:Schema(description = "공지 제목", example = "2024학년도 1학기 휴업일 안내")
    val title: String,
    @get:Schema(description = "공지 URL", example = "https://www.hyu.ac.kr/notice/12345")
    val url: String,
    @get:Schema(description = "공지 만료 일자", example = "2024-03-01 23:59:59")
    val expiredAt: String,
    @get:Schema(description = "공지 작성자 ID", example = "admin")
    val userID: String,
    @get:Schema(description = "공지 언어", example = "KOREAN")
    val language: String,
)
