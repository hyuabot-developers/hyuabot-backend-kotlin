package app.hyuabot.backend.contact.domain

import io.swagger.v3.oas.annotations.media.Schema

data class ContactResponse(
    @get:Schema(description = "전화부 ID", example = "1")
    val seq: Int,
    @get:Schema(description = "전화부 카테고리 ID", example = "1")
    val categoryID: Int,
    @get:Schema(description = "전화부 캠퍼스 ID", example = "1")
    val campusID: Int,
    @get:Schema(description = "전화부 이름", example = "홍길동")
    val name: String,
    @get:Schema(description = "전화부 전화번호", example = "010-1234-5678")
    val phone: String,
)
