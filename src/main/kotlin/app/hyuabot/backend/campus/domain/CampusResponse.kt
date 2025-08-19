package app.hyuabot.backend.campus.domain

import io.swagger.v3.oas.annotations.media.Schema

data class CampusResponse(
    @get:Schema(description = "캠퍼스 ID", example = "1")
    val seq: Int,
    @get:Schema(description = "캠퍼스 이름", example = "서울캠퍼스")
    val name: String,
)
