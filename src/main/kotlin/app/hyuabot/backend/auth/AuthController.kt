package app.hyuabot.backend.auth

import app.hyuabot.backend.auth.domain.CreateUserRequest
import app.hyuabot.backend.auth.domain.LoginRequest
import app.hyuabot.backend.auth.domain.TokenResponse
import app.hyuabot.backend.auth.domain.UserResponse
import app.hyuabot.backend.security.JWTUser
import app.hyuabot.backend.utility.ResponseBuilder
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.jvm.javaClass

@RequestMapping(path = ["/api/v1/user"])
@RestController
@Tag(name = "Auth", description = "사용자 인증 및 관리 API")
class AuthController(
    private val authService: AuthService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

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

    @PostMapping("/token", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    @Operation(
        summary = "로그인",
        description = "사용자 로그인 API. 사용자 ID와 비밀번호를 사용하여 JWT 토큰을 발급합니다.",
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content =
                    arrayOf(
                        Content(
                            mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                            schema = Schema(implementation = LoginRequest::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "LoginRequestExample",
                                        description = "로그인 요청 예시",
                                        value = "username=user123&password=securePassword123",
                                    ),
                                ),
                        ),
                    ),
            ),
    )
    fun login(
        @ModelAttribute payload: LoginRequest,
    ): ResponseEntity<TokenResponse> {
        authService.login(payload.username, payload.password).let {
            return ResponseBuilder.response(
                HttpStatus.CREATED,
                it,
            )
        }
    }

    @DeleteMapping("/token")
    @Operation(
        summary = "로그아웃",
        description = "사용자 로그아웃 API. 현재 로그인된 사용자의 JWT 토큰을 무효화합니다.",
    )
    fun logout(): ResponseEntity<ResponseBuilder.Message> {
        val userID = (SecurityContextHolder.getContext().authentication.principal as JWTUser).username
        val userInfo = authService.getUserInfo(userID)
        authService.logout(userInfo).let {
            return ResponseBuilder.response(HttpStatus.OK, "LOGOUT_SUCCESS")
        }
    }

    @GetMapping("/profile")
    @Operation(
        summary = "사용자 프로필 조회",
        description = "로그인된 사용자의 프로필 정보를 조회합니다.",
    )
    fun getProfile(): ResponseEntity<UserResponse> {
        val userID = (SecurityContextHolder.getContext().authentication.principal as JWTUser).username
        return try {
            authService.getUserInfo(userID).let { user ->
                ResponseBuilder.response(
                    HttpStatus.OK,
                    UserResponse(
                        username = user.userID,
                        nickname = user.name,
                        email = user.email,
                        phone = user.phone,
                        active = user.active,
                    ),
                )
            }
        } catch (e: IllegalArgumentException) {
            throw ResponseBuilder.exception(HttpStatus.NOT_FOUND, e.message ?: "NO_USER_INFO")
        }
    }
}
