package app.hyuabot.backend.auth.domain

import io.swagger.v3.oas.annotations.media.Schema

data class CreateUserRequest(
    @get:Schema(description = "사용자 ID", example = "user123")
    val userID: String,
    @get:Schema(description = "닉네임", example = "UserNickname")
    val nickname: String,
    @get:Schema(description = "사용자 이메일", example = "admin@example.com")
    val email: String,
    @get:Schema(description = "사용자 비밀번호", example = "securePassword123")
    val password: String,
    @get:Schema(description = "사용자 핸드폰 번호", example = "+821012345678")
    val phone: String,
)
