package app.hyuabot.backend.admin.domain

import app.hyuabot.backend.security.AdminPermission

data class CreateAdminUserRequest(
    val userID: String,
    val nickname: String,
    val email: String,
    val phone: String,
    val permissions: Set<AdminPermission>,
)
