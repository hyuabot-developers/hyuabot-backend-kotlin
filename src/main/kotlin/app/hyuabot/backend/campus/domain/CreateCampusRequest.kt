package app.hyuabot.backend.campus.domain

import io.swagger.v3.oas.annotations.media.Schema

data class CreateCampusRequest(
    @get:Schema(description = "캠퍼스 이름", example = "TEST")
    val name: String,
)
