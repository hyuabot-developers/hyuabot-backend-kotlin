package app.hyuabot.backend.auth.domain

import io.swagger.v3.oas.annotations.media.Schema

data class UserResponse(
    @get:Schema(description = "사용자 ID", example = "user123")
    val username: String,
    @get:Schema(description = "닉네임", example = "UserNickname")
    val nickname: String,
    @get:Schema(description = "사용자 이메일", example = "test@example.com")
    val email: String,
    @get:Schema(description = "사용자 핸드폰 번호", example = "+821012345678")
    val phone: String,
    @get:Schema(description = "사용자 활성화 상태", example = "true")
    val active: Boolean,
)
