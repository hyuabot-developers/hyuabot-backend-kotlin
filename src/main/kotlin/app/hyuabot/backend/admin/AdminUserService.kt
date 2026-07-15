package app.hyuabot.backend.admin

import app.hyuabot.backend.admin.domain.AdminUserResponse
import app.hyuabot.backend.admin.domain.UpdateAdminUserRequest
import app.hyuabot.backend.admin.exception.AdminUserNotFoundException
import app.hyuabot.backend.admin.exception.LastSuperAdminException
import app.hyuabot.backend.database.repository.UserRepository
import app.hyuabot.backend.security.AdminPermission
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class AdminUserService(
    private val userRepository: UserRepository,
) {
    fun getUsers(): List<AdminUserResponse> = userRepository.findAllByOrderByNameAscUserIDAsc().map(AdminUserResponse::from)

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

        user.active = request.active
        user.permissions.clear()
        user.permissions.addAll(request.permissions)
        return AdminUserResponse.from(userRepository.save(user))
    }
}
