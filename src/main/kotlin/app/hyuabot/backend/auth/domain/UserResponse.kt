package app.hyuabot.backend.auth.domain

data class UserResponse(
    val username: String,
    val nickname: String,
    val email: String,
    val phone: String,
    val active: Boolean,
)
