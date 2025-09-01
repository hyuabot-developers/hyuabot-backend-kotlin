package app.hyuabot.backend.contact.domain

import io.swagger.v3.oas.annotations.media.Schema

data class ContactCategoryRequest(
    @get:Schema(description = "전화부 카테고리 이름", example = "학과")
    val name: String,
)
