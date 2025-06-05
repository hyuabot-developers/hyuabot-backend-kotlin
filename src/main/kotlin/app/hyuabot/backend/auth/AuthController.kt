package app.hyuabot.backend.auth

import app.hyuabot.backend.auth.domain.CreateUserRequest
import app.hyuabot.backend.utility.ResponseBuilder
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping(path = ["/api/v1/auth"])
@RestController
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/user")
    fun signUp(
        @RequestBody payload: CreateUserRequest,
    ): ResponseEntity<ResponseBuilder.Message> {
        try {
            authService.signUp(payload)
            return ResponseBuilder.response(HttpStatus.CREATED, "USER_CREATED_SUCCESSFULLY")
        } catch (e: DuplicateKeyException) {
            throw ResponseBuilder.exception(HttpStatus.CONFLICT, e.message ?: "")
        } catch (e: Exception) {
            throw ResponseBuilder.exception(HttpStatus.INTERNAL_SERVER_ERROR, e.message ?: "INTERNAL_SERVER_ERROR")
        }
    }
}
