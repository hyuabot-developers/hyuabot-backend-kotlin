package app.hyuabot.backend.admin

import app.hyuabot.backend.admin.domain.AdminUserInvitationResponse
import app.hyuabot.backend.admin.domain.AdminUserResponse
import app.hyuabot.backend.admin.domain.CreateAdminUserRequest
import app.hyuabot.backend.admin.domain.UpdateAdminUserRequest
import app.hyuabot.backend.admin.exception.AdminUserNotFoundException
import app.hyuabot.backend.admin.exception.LastSuperAdminException
import app.hyuabot.backend.admin.exception.PendingUserActivationException
import app.hyuabot.backend.auth.UserInvitationService
import app.hyuabot.backend.auth.exception.DuplicateEmailException
import app.hyuabot.backend.auth.exception.DuplicateUserIDException
import app.hyuabot.backend.auth.exception.InvalidUserInputException
import app.hyuabot.backend.database.entity.User
import app.hyuabot.backend.database.repository.AdminUserInvitationRepository
import app.hyuabot.backend.database.repository.UserRepository
import app.hyuabot.backend.security.AdminPermission
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class AdminUserService(
    private val userRepository: UserRepository,
    private val invitationRepository: AdminUserInvitationRepository,
    private val invitationService: UserInvitationService,
) {
    fun getUsers(): List<AdminUserResponse> {
        val invitations =
            invitationRepository
                .findAllByConsumedAtIsNullAndRevokedAtIsNull()
                .associateBy({ it.userID }, { it.expiresAt })
        val now = ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone)
        return userRepository.findAllByOrderByNameAscUserIDAsc().map { user ->
            AdminUserResponse.from(user, invitations[user.userID], now)
        }
    }

    @Transactional
    fun createUser(
        request: CreateAdminUserRequest,
        createdBy: String,
    ): AdminUserInvitationResponse {
        validate(request)
        if (userRepository.findByUserID(request.userID.trim()) != null) throw DuplicateUserIDException()
        val email = request.email.trim().lowercase()
        if (userRepository.findByEmailIgnoreCase(email) != null) throw DuplicateEmailException()

        val user =
            userRepository.saveAndFlush(
                User(
                    userID = request.userID.trim(),
                    password = null,
                    name = request.nickname.trim(),
                    email = email,
                    phone = request.phone.trim(),
                    active = false,
                    permissions = request.permissions.toMutableSet(),
                ),
            )
        val invitation = invitationService.issue(user.userID, createdBy)
        return AdminUserInvitationResponse(
            user = AdminUserResponse.from(user, invitation.expiresAt),
            token = invitation.token,
            expiresAt = invitation.expiresAt,
        )
    }

    fun reissueInvitation(
        userID: String,
        createdBy: String,
    ): AdminUserInvitationResponse {
        val user = userRepository.findByUserID(userID) ?: throw AdminUserNotFoundException()
        val invitation = invitationService.issue(userID, createdBy)
        return AdminUserInvitationResponse(
            user = AdminUserResponse.from(user, invitation.expiresAt),
            token = invitation.token,
            expiresAt = invitation.expiresAt,
        )
    }

    @Transactional
    fun updateUser(
        userID: String,
        request: UpdateAdminUserRequest,
    ): AdminUserResponse {
        val users = userRepository.findAllForPermissionUpdate()
        val user = users.firstOrNull { it.userID == userID } ?: throw AdminUserNotFoundException()
        val removesActiveSuperAdmin =
            user.active &&
                AdminPermission.SUPER_ADMIN in user.permissions &&
                (!request.active || AdminPermission.SUPER_ADMIN !in request.permissions)
        if (
            removesActiveSuperAdmin &&
            users.count { it.active && AdminPermission.SUPER_ADMIN in it.permissions } <= 1
        ) {
            throw LastSuperAdminException()
        }
        if (request.active && user.password == null) {
            throw PendingUserActivationException()
        }

        user.active = request.active
        user.permissions.clear()
        user.permissions.addAll(request.permissions)
        return AdminUserResponse.from(userRepository.save(user))
    }

    private fun validate(request: CreateAdminUserRequest) {
        if (
            request.userID.trim().isEmpty() ||
            request.userID.trim().length > 20 ||
            request.nickname.trim().isEmpty() ||
            request.nickname.trim().length > 20 ||
            request.email.trim().isEmpty() ||
            request.email.trim().length > 50 ||
            request.phone.trim().length > 15
        ) {
            throw InvalidUserInputException("INVALID_USER_INPUT")
        }
    }
}
