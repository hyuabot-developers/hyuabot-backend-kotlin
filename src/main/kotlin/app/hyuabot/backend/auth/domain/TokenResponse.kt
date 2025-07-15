package app.hyuabot.backend.auth.domain

data class TokenResponse(
    val grantType: String = "Bearer",
    val accessToken: String,
    val refreshToken: String,
)
