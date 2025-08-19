package app.hyuabot.backend.auth.domain

import io.swagger.v3.oas.annotations.media.Schema

data class TokenResponse(
    val grantType: String = "Bearer",
    @get:Schema(description = "Access Token", example = "access_token")
    val accessToken: String,
    @get:Schema(description = "Refresh Token", example = "refresh_token")
    val refreshToken: String,
)
