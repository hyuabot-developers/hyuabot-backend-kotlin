package app.hyuabot.backend.contact.domain

import io.swagger.v3.oas.annotations.media.Schema

data class ContactCategoryResponse(
    @get:Schema(description = "전화부 카테고리 ID", example = "1")
    val seq: Int,
    @get:Schema(description = "전화부 카테고리 이름", example = "학과")
    val name: String,
)
