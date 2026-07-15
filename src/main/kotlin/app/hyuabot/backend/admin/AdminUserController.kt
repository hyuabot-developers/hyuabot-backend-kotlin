package app.hyuabot.backend.admin

import app.hyuabot.backend.admin.domain.AdminUserListResponse
import app.hyuabot.backend.admin.domain.CreateAdminUserRequest
import app.hyuabot.backend.admin.domain.UpdateAdminUserRequest
import app.hyuabot.backend.admin.exception.AdminUserNotFoundException
import app.hyuabot.backend.admin.exception.LastSuperAdminException
import app.hyuabot.backend.admin.exception.PendingUserActivationException
import app.hyuabot.backend.auth.exception.DuplicateEmailException
import app.hyuabot.backend.auth.exception.DuplicateUserIDException
import app.hyuabot.backend.auth.exception.InvalidInvitationException
import app.hyuabot.backend.auth.exception.InvalidUserInputException
import app.hyuabot.backend.security.JWTUser
import app.hyuabot.backend.utility.ResponseBuilder
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/users")
class AdminUserController(
    private val adminUserService: AdminUserService,
) {
    @GetMapping
    fun getUsers(): ResponseEntity<AdminUserListResponse> =
        ResponseBuilder.response(
            HttpStatus.OK,
            AdminUserListResponse(adminUserService.getUsers()),
        )

    @PostMapping
    fun createUser(
        @RequestBody request: CreateAdminUserRequest,
    ): ResponseEntity<*> =
        try {
            ResponseBuilder.response(
                HttpStatus.CREATED,
                adminUserService.createUser(request, currentUserID()),
            )
        } catch (_: DuplicateUserIDException) {
            ResponseBuilder.response(HttpStatus.CONFLICT, "DUPLICATE_USER_ID")
        } catch (_: DuplicateEmailException) {
            ResponseBuilder.response(HttpStatus.CONFLICT, "DUPLICATE_EMAIL")
        } catch (exception: InvalidUserInputException) {
            ResponseBuilder.response(HttpStatus.BAD_REQUEST, exception.code)
        }

    @PostMapping("/{userID}/invitation")
    fun reissueInvitation(
        @PathVariable userID: String,
    ): ResponseEntity<*> =
        try {
            ResponseBuilder.response(
                HttpStatus.OK,
                adminUserService.reissueInvitation(userID, currentUserID()),
            )
        } catch (_: AdminUserNotFoundException) {
            ResponseBuilder.response(HttpStatus.NOT_FOUND, "ADMIN_USER_NOT_FOUND")
        } catch (_: InvalidInvitationException) {
            ResponseBuilder.response(HttpStatus.CONFLICT, "USER_ALREADY_SETUP")
        }

    @PutMapping("/{userID}")
    fun updateUser(
        @PathVariable userID: String,
        @RequestBody request: UpdateAdminUserRequest,
    ): ResponseEntity<*> =
        try {
            ResponseBuilder.response(
                HttpStatus.OK,
                adminUserService.updateUser(userID, request),
            )
        } catch (_: AdminUserNotFoundException) {
            ResponseBuilder.response(HttpStatus.NOT_FOUND, "ADMIN_USER_NOT_FOUND")
        } catch (_: LastSuperAdminException) {
            ResponseBuilder.response(HttpStatus.CONFLICT, "LAST_SUPER_ADMIN_REQUIRED")
        } catch (_: PendingUserActivationException) {
            ResponseBuilder.response(HttpStatus.CONFLICT, "USER_SETUP_REQUIRED")
        }

    private fun currentUserID(): String = (SecurityContextHolder.getContext().authentication?.principal as JWTUser).username
}
