package app.hyuabot.backend.auth.domain

data class LoginRequest(
    val username: String,
    val password: String,
)
