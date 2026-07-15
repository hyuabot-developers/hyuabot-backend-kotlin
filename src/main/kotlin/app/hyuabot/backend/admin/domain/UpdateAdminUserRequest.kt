package app.hyuabot.backend.admin.domain

import app.hyuabot.backend.security.AdminPermission

data class UpdateAdminUserRequest(
    val active: Boolean,
    val permissions: Set<AdminPermission>,
)
