package app.hyuabot.backend.admin.domain

import app.hyuabot.backend.database.entity.User
import app.hyuabot.backend.security.AdminPermission
import java.time.ZonedDateTime

enum class AdminUserStatus {
    DELETED,
    PENDING_SETUP,
    INVITATION_EXPIRED,
    ACTIVE,
    INACTIVE,
}

data class AdminUserResponse(
    val username: String,
    val nickname: String,
    val email: String,
    val phone: String,
    val active: Boolean,
    val status: AdminUserStatus = AdminUserStatus.ACTIVE,
    val invitationExpiresAt: ZonedDateTime? = null,
    val permissions: List<AdminPermission>,
) {
    companion object {
        fun from(
            user: User,
            invitationExpiresAt: ZonedDateTime? = null,
            now: ZonedDateTime = ZonedDateTime.now(),
        ): AdminUserResponse =
            AdminUserResponse(
                username = user.userID,
                nickname = user.name,
                email = user.email,
                phone = user.phone,
                active = user.active,
                status =
                    when {
                        user.deletedAt != null -> AdminUserStatus.DELETED
                        user.password == null && invitationExpiresAt?.isAfter(now) == true ->
                            AdminUserStatus.PENDING_SETUP
                        user.password == null -> AdminUserStatus.INVITATION_EXPIRED
                        user.active -> AdminUserStatus.ACTIVE
                        else -> AdminUserStatus.INACTIVE
                    },
                invitationExpiresAt = invitationExpiresAt,
                permissions = user.permissions.sortedBy { it.ordinal },
            )
    }
}

data class AdminUserListResponse(
    val result: List<AdminUserResponse>,
)

data class AdminUserInvitationResponse(
    val user: AdminUserResponse,
    val token: String,
    val expiresAt: ZonedDateTime,
)
