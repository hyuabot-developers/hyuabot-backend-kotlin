package app.hyuabot.backend.admin

import app.hyuabot.backend.admin.domain.AdminUserListResponse
import app.hyuabot.backend.admin.domain.UpdateAdminUserRequest
import app.hyuabot.backend.admin.exception.AdminUserNotFoundException
import app.hyuabot.backend.admin.exception.LastSuperAdminException
import app.hyuabot.backend.utility.ResponseBuilder
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
        }
}
