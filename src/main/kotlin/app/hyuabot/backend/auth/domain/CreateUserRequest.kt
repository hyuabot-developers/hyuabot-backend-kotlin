package app.hyuabot.backend.auth.domain

data class CreateUserRequest(
    val userID: String,
    val nickname: String,
    val email: String,
    val password: String,
    val phone: String,
)
