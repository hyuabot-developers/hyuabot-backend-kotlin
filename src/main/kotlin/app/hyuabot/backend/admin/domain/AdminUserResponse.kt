package app.hyuabot.backend.admin.domain

import app.hyuabot.backend.database.entity.User
import app.hyuabot.backend.security.AdminPermission

data class AdminUserResponse(
    val username: String,
    val nickname: String,
    val email: String,
    val phone: String,
    val active: Boolean,
    val permissions: List<AdminPermission>,
) {
    companion object {
        fun from(user: User): AdminUserResponse =
            AdminUserResponse(
                username = user.userID,
                nickname = user.name,
                email = user.email,
                phone = user.phone,
                active = user.active,
                permissions = user.permissions.sortedBy { it.ordinal },
            )
    }
}

data class AdminUserListResponse(
    val result: List<AdminUserResponse>,
)
