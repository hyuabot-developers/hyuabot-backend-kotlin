package app.hyuabot.backend.auth

import app.hyuabot.backend.auth.domain.ChangePasswordRequest
import app.hyuabot.backend.auth.domain.CompleteInvitationRequest
import app.hyuabot.backend.auth.domain.LoginRequest
import app.hyuabot.backend.auth.domain.UpdateProfileRequest
import app.hyuabot.backend.auth.domain.UserResponse
import app.hyuabot.backend.auth.domain.ValidateInvitationRequest
import app.hyuabot.backend.auth.exception.DuplicateEmailException
import app.hyuabot.backend.auth.exception.InvalidInvitationException
import app.hyuabot.backend.auth.exception.InvalidUserInputException
import app.hyuabot.backend.security.AuthCookieProvider
import app.hyuabot.backend.security.JWTUser
import app.hyuabot.backend.security.effectivePermissions
import app.hyuabot.backend.utility.ResponseBuilder
import io.jsonwebtoken.ExpiredJwtException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.jvm.javaClass

@RequestMapping(path = ["/api/v1/user"])
@RestController
@Tag(name = "Auth", description = "사용자 인증 및 관리 API")
class AuthController {
    @Autowired private lateinit var authService: AuthService

    @Autowired private lateinit var invitationService: UserInvitationService

    @Autowired private lateinit var authCookieProvider: AuthCookieProvider
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/account-setup/validate")
    fun validateInvitation(
        @RequestBody request: ValidateInvitationRequest,
    ) = ResponseBuilder.response(HttpStatus.OK, invitationService.validate(request.token))

    @PostMapping("/account-setup/complete")
    fun completeInvitation(
        @RequestBody request: CompleteInvitationRequest,
    ): ResponseEntity<ResponseBuilder.Message> =
        try {
            invitationService.complete(request.token, request.password)
            ResponseBuilder.response(HttpStatus.OK, "ACCOUNT_SETUP_COMPLETE")
        } catch (exception: InvalidUserInputException) {
            ResponseBuilder.response(HttpStatus.BAD_REQUEST, exception.code)
        } catch (_: InvalidInvitationException) {
            ResponseBuilder.response(HttpStatus.BAD_REQUEST, "INVALID_OR_EXPIRED_INVITATION")
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
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "로그인 성공, JWT 토큰 발급",
                content =
                    arrayOf(
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "LOGIN_SUCCESS",
                                        description = "로그인 성공 예시",
                                        value = "{\"message\": \"LOGIN_SUCCESS\"}",
                                    ),
                                ),
                        ),
                    ),
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패, 잘못된 사용자 ID 또는 비밀번호",
                content =
                    arrayOf(
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "UNAUTHORIZED",
                                        description = "인증 실패 예시",
                                        value = "{\"message\": \"UNAUTHORIZED\"}",
                                    ),
                                ),
                        ),
                    ),
            ),
        ],
    )
    fun login(
        @ModelAttribute payload: LoginRequest,
    ): ResponseEntity<ResponseBuilder.Message> {
        try {
            authService.login(payload.username, payload.password).let {
                val accessTokenCookie = authCookieProvider.accessTokenCookie(it.accessToken)
                val refreshTokenCookie = authCookieProvider.refreshTokenCookie(it.refreshToken)
                return ResponseBuilder
                    .response(
                        HttpStatus.CREATED,
                        "LOGIN_SUCCESS",
                        cookies = listOf(accessTokenCookie, refreshTokenCookie),
                    )
            }
        } catch (_: BadCredentialsException) {
            return ResponseBuilder.response(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED")
        } catch (e: Exception) {
            logger.error("Login failed: ${e.message}", e)
            return ResponseBuilder.response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR")
        }
    }

    @PutMapping("/token")
    @Operation(
        summary = "토큰 갱신",
        description = "사용자 JWT 토큰을 갱신합니다. 현재 로그인된 사용자의 JWT 토큰을 새로 발급합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "토큰 갱신 성공",
                content =
                    arrayOf(
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "TOKEN_REFRESH_SUCCESS",
                                        description = "토큰 갱신 성공 예시",
                                        value = "{\"message\": \"TOKEN_REFRESH_SUCCESS\"}",
                                    ),
                                ),
                        ),
                    ),
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증되지 않은 사용자, 로그인이 필요합니다.",
                content =
                    arrayOf(
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "UNAUTHORIZED",
                                        description = "인증되지 않은 사용자 예시",
                                        value = "{\"message\": \"UNAUTHORIZED\"}",
                                    ),
                                ),
                        ),
                    ),
            ),
        ],
    )
    fun refreshToken(request: HttpServletRequest): ResponseEntity<ResponseBuilder.Message> {
        if (request.cookies == null || request.cookies.firstOrNull { it.name == "refresh_token" } == null) {
            return ResponseBuilder.response(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED")
        }
        val refreshToken = request.cookies.first { it.name == "refresh_token" }.value
        try {
            val newAccessToken = authService.refreshToken(refreshToken)
            val accessTokenCookie = authCookieProvider.accessTokenCookie(newAccessToken)
            return ResponseBuilder.response(
                HttpStatus.OK,
                "TOKEN_REFRESH_SUCCESS",
                cookies = listOf(accessTokenCookie),
            )
        } catch (_: ExpiredJwtException) {
            return ResponseBuilder.response(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED")
        } catch (_: BadCredentialsException) {
            return ResponseBuilder.response(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED")
        } catch (e: Exception) {
            logger.error("Token refresh failed: ${e.message}", e)
            return ResponseBuilder.response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR")
        }
    }

    @DeleteMapping("/token")
    @Operation(
        summary = "로그아웃",
        description = "사용자 로그아웃 API. 현재 로그인된 사용자의 JWT 토큰을 무효화합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "로그아웃 성공",
                content =
                    arrayOf(
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "LOGOUT_SUCCESS",
                                        description = "로그아웃 성공 예시",
                                        value = "{\"message\": \"LOGOUT_SUCCESS\"}",
                                    ),
                                ),
                        ),
                    ),
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증되지 않은 사용자, 로그인이 필요합니다.",
                content =
                    arrayOf(
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "UNAUTHORIZED",
                                        description = "인증되지 않은 사용자 예시",
                                        value = "{\"message\": \"UNAUTHORIZED\"}",
                                    ),
                                ),
                        ),
                    ),
            ),
        ],
    )
    fun logout(request: HttpServletRequest): ResponseEntity<ResponseBuilder.Message> {
        val authentication = SecurityContextHolder.getContext().authentication!!
        val principal = authentication.principal
        if (principal !is JWTUser) {
            return ResponseBuilder.response(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED")
        }
        val userID = principal.username
        val userInfo = authService.getUserInfo(userID)
        authService.logout(userInfo, request).let {
            return ResponseBuilder.response(
                HttpStatus.OK,
                "LOGOUT_SUCCESS",
                cookies = authCookieProvider.expiredAuthCookies(),
            )
        }
    }

    @GetMapping("/profile")
    @Operation(
        summary = "사용자 프로필 조회",
        description = "로그인된 사용자의 프로필 정보를 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "사용자 프로필 조회 성공",
                content =
                    arrayOf(
                        Content(schema = Schema(implementation = UserResponse::class)),
                    ),
            ),
            ApiResponse(
                responseCode = "404",
                description = "사용자 정보가 존재하지 않음",
                content =
                    arrayOf(
                        Content(
                            schema = Schema(implementation = ResponseBuilder.Message::class),
                            examples =
                                arrayOf(
                                    ExampleObject(
                                        name = "NO_USER_INFO",
                                        description = "사용자 정보가 존재하지 않는 경우 예시",
                                        value = "{\"message\": \"NO_USER_INFO\"}",
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
    fun getProfile(): ResponseEntity<*> {
        val authentication = SecurityContextHolder.getContext().authentication!!
        val principal = authentication.principal
        if (principal !is JWTUser) {
            return ResponseBuilder.response(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED")
        }
        val userID = principal.username
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
                        permissions = user.permissions.effectivePermissions().sortedBy { it.ordinal },
                    ),
                )
            }
        } catch (_: IllegalArgumentException) {
            return ResponseBuilder.response(HttpStatus.NOT_FOUND, "NO_USER_INFO")
        }
    }

    @PatchMapping("/profile")
    fun updateProfile(
        @RequestBody request: UpdateProfileRequest,
    ): ResponseEntity<*> =
        try {
            val user = authService.updateProfile(currentUserID(), request)
            ResponseBuilder.response(HttpStatus.OK, userResponse(user))
        } catch (_: DuplicateEmailException) {
            ResponseBuilder.response(HttpStatus.CONFLICT, "DUPLICATE_EMAIL")
        } catch (exception: InvalidUserInputException) {
            ResponseBuilder.response(HttpStatus.BAD_REQUEST, exception.code)
        }

    @PutMapping("/password")
    fun changePassword(
        @RequestBody request: ChangePasswordRequest,
    ): ResponseEntity<ResponseBuilder.Message> =
        try {
            authService.changePassword(currentUserID(), request)
            ResponseBuilder.response(
                HttpStatus.OK,
                "PASSWORD_CHANGED",
                cookies = authCookieProvider.expiredAuthCookies(),
            )
        } catch (_: BadCredentialsException) {
            ResponseBuilder.response(HttpStatus.BAD_REQUEST, "CURRENT_PASSWORD_MISMATCH")
        } catch (exception: InvalidUserInputException) {
            ResponseBuilder.response(HttpStatus.BAD_REQUEST, exception.code)
        }

    private fun currentUserID(): String = (SecurityContextHolder.getContext().authentication?.principal as JWTUser).username

    private fun userResponse(user: app.hyuabot.backend.database.entity.User) =
        UserResponse(
            username = user.userID,
            nickname = user.name,
            email = user.email,
            phone = user.phone,
            active = user.active,
            permissions = user.permissions.effectivePermissions().sortedBy { it.ordinal },
        )
}
