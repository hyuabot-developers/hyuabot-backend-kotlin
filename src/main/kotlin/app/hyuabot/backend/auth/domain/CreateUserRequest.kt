package app.hyuabot.backend.auth.domain

import io.swagger.v3.oas.annotations.media.Schema

data class CreateUserRequest(
    @Schema(description = "User ID", example = "user123")
    val userID: String,
    @Schema(description = "Nickname", example = "UserNickname")
    val nickname: String,
    @Schema(description = "Email address", example = "admin@example.com")
    val email: String,
    @Schema(description = "Password", example = "securePassword123")
    val password: String,
    @Schema(description = "Phone number", example = "+821012345678")
    val phone: String,
)
