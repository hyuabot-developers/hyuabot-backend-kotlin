package app.hyuabot.backend.auth.domain

import io.swagger.v3.oas.annotations.media.Schema

data class LoginRequest(
    @get:Schema(description = "사용자 이름", example = "user123")
    val username: String,
    @get:Schema(description = "사용자 비밀번호", example = "securePassword123")
    val password: String,
)
