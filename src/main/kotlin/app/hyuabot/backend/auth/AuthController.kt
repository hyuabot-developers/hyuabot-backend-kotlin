package app.hyuabot.backend.auth

import app.hyuabot.backend.auth.domain.CreateUserRequest
import app.hyuabot.backend.utility.ResponseBuilder
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping(path = ["/api/v1/user"])
@RestController
@Tag(name = "Auth", description = "사용자 인증 및 관리 API")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("")
    @Operation(summary = "회원가입", description = "사용자 회원가입 API")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "사용자 회원가입 성공",
                content =
                    arrayOf(
                        Content(
                            schema =
                                Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "USER_CREATED_SUCCESSFULLY",
                                        description = "사용자 회원가입 성공 예시",
                                        value = "{\"message\": \"USER_CREATED_SUCCESSFULLY\"}",
                                    ),
                                ),
                        ),
                    ),
            ),
            ApiResponse(
                responseCode = "409",
                description = "사용자 ID 또는 이메일 중복",
                content =
                    arrayOf(
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "DUPLICATE_USER_ID",
                                        description = "사용자 ID 중복 예시",
                                        value = "{\"message\": \"DUPLICATE_USER_ID\"}",
                                    ),
                                    ExampleObject(
                                        name = "DUPLICATE_EMAIL",
                                        description = "이메일 중복 예시",
                                        value = "{\"message\": \"DUPLICATE_EMAIL\"}",
                                    ),
                                ),
                        ),
                    ),
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content =
                    arrayOf(
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "INTERNAL_SERVER_ERROR",
                                        description = "서버 내부 오류 예시",
                                        value = "{\"message\": \"INTERNAL_SERVER_ERROR\"}",
                                    ),
                                ),
                        ),
                    ),
            ),
        ],
    )
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
