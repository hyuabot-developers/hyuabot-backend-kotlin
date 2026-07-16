package app.hyuabot.backend.auth

import app.hyuabot.backend.auth.domain.ChangePasswordRequest
import app.hyuabot.backend.auth.domain.InvitationValidationResponse
import app.hyuabot.backend.auth.domain.TokenResponse
import app.hyuabot.backend.auth.domain.UpdateProfileRequest
import app.hyuabot.backend.auth.exception.DuplicateEmailException
import app.hyuabot.backend.auth.exception.InvalidInvitationException
import app.hyuabot.backend.auth.exception.InvalidUserInputException
import app.hyuabot.backend.database.entity.User
import app.hyuabot.backend.database.repository.UserRepository
import app.hyuabot.backend.security.AdminPermission
import app.hyuabot.backend.security.WithCustomMockUser
import io.jsonwebtoken.ExpiredJwtException
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.time.ZonedDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {
    @Autowired
    private lateinit var controller: AuthController

    @MockitoBean
    private lateinit var authService: AuthService

    @MockitoBean
    private lateinit var invitationService: UserInvitationService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        userRepository.save(
            User(
                userID = "test_user",
                password = "password".toByteArray(),
                name = "Test User",
                email = "test@example.com",
                phone = "1234567890",
                active = true,
            ),
        )
    }

    @AfterEach
    fun tearDown() {
        userRepository.deleteAll()
    }

    @Test
    @DisplayName("로그인 테스트 (정상)")
    fun testLogin() {
        doReturn(
            TokenResponse(
                accessToken = "mockAccessToken",
                refreshToken = "mockRefreshToken",
            ),
        ).whenever(authService)
            .login("test_user", "password")
        val map: Map<String, String> =
            mapOf(
                "username" to "test_user",
                "password" to "password",
            )
        mockMvc
            .perform(
                post("/api/v1/user/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .content("username=${map["username"]}&password=${map["password"]}"),
            ).andExpect(status().isCreated)
            .andExpect(cookie().exists("access_token"))
            .andExpect(cookie().exists("refresh_token"))
            .andExpect(jsonPath("$.message").value("LOGIN_SUCCESS"))
    }

    @Test
    fun validateAndCompleteInvitationArePublic() {
        val expiry = ZonedDateTime.now().plusHours(1)
        whenever(invitationService.validate("token")).thenReturn(InvitationValidationResponse(true, expiry))

        mockMvc
            .perform(
                post("/api/v1/user/account-setup/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\":\"token\"}"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.valid").value(true))

        mockMvc
            .perform(
                post("/api/v1/user/account-setup/complete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\":\"token\",\"password\":\"Password1!\"}"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("ACCOUNT_SETUP_COMPLETE"))
    }

    @Test
    fun completeInvitationMapsInvalidPasswordAndToken() {
        doThrow(InvalidUserInputException("INVALID_PASSWORD"))
            .whenever(invitationService)
            .complete("token", "short")
        mockMvc
            .perform(
                post("/api/v1/user/account-setup/complete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\":\"token\",\"password\":\"short\"}"),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("INVALID_PASSWORD"))

        doThrow(InvalidInvitationException())
            .whenever(invitationService)
            .complete("expired", "Password1!")
        mockMvc
            .perform(
                post("/api/v1/user/account-setup/complete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\":\"expired\",\"password\":\"Password1!\"}"),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("INVALID_OR_EXPIRED_INVITATION"))
    }

    @Test
    @DisplayName("로그인 테스트 (실패)")
    fun testLoginFailure() {
        doThrow(BadCredentialsException("INVALID_CREDENTIALS"))
            .whenever(authService)
            .login("test_user", "wrong_password")
        val map: Map<String, String> =
            mapOf(
                "username" to "test_user",
                "password" to "wrong_password",
            )
        mockMvc
            .perform(
                post("/api/v1/user/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .content("username=${map["username"]}&password=${map["password"]}"),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("UNAUTHORIZED"))
    }

    @Test
    @DisplayName("로그인 테스트 (오류)")
    fun testLoginError() {
        doThrow(RuntimeException("DB_ERROR"))
            .whenever(authService)
            .login("test_user", "password")
        val map: Map<String, String> =
            mapOf(
                "username" to "test_user",
                "password" to "password",
            )
        mockMvc
            .perform(
                post("/api/v1/user/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .content("username=${map["username"]}&password=${map["password"]}"),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("토큰 갱신 테스트 (정상)")
    fun testRefreshToken() {
        doReturn("newAccessToken")
            .whenever(authService)
            .refreshToken("mockRefreshToken")
        mockMvc
            .perform(
                put("/api/v1/user/token")
                    .cookie(Cookie("refresh_token", "mockRefreshToken")),
            ).andExpect(status().isOk)
            .andExpect(cookie().exists("access_token"))
            .andExpect(jsonPath("$.message").value("TOKEN_REFRESH_SUCCESS"))
    }

    @Test
    @DisplayName("토큰 갱신 테스트 (토큰 없음)")
    fun testRefreshTokenWithoutCookie() {
        mockMvc
            .perform(
                put("/api/v1/user/token"),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("UNAUTHORIZED"))
    }

    @Test
    @DisplayName("토큰 갱신 테스트 (Refresh Token 없음)")
    fun testRefreshTokenWithoutRefreshToken() {
        mockMvc
            .perform(
                put("/api/v1/user/token")
                    .cookie(Cookie("test", "testCookie")),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("UNAUTHORIZED"))
    }

    @Test
    @DisplayName("토큰 갱신 테스트 (만료된 토큰)")
    fun testRefreshTokenWithExpiredToken() {
        doThrow(ExpiredJwtException(null, null, "TOKEN_EXPIRED"))
            .whenever(authService)
            .refreshToken("expiredRefreshToken")
        mockMvc
            .perform(
                put("/api/v1/user/token")
                    .cookie(Cookie("refresh_token", "expiredRefreshToken")),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("UNAUTHORIZED"))
    }

    @Test
    @DisplayName("토큰 갱신 테스트 (잘못된 토큰)")
    fun testRefreshTokenWithInvalidToken() {
        doThrow(BadCredentialsException("INVALID_REFRESH_TOKEN"))
            .whenever(authService)
            .refreshToken("invalidRefreshToken")
        mockMvc
            .perform(
                put("/api/v1/user/token")
                    .cookie(Cookie("refresh_token", "invalidRefreshToken")),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("UNAUTHORIZED"))
    }

    @Test
    @DisplayName("토큰 갱신 테스트 (오류)")
    fun testRefreshTokenError() {
        doThrow(RuntimeException("DB_ERROR"))
            .whenever(authService)
            .refreshToken("mockRefreshToken")
        mockMvc
            .perform(
                put("/api/v1/user/token")
                    .cookie(Cookie("refresh_token", "mockRefreshToken")),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("로그아웃 테스트 (정상)")
    @WithCustomMockUser(username = "test_user")
    fun testLogout() {
        mockMvc
            .perform(
                delete("/api/v1/user/token")
                    .cookie(Cookie("access_token", "mockAccessToken"))
                    .cookie(Cookie("refresh_token", "mockRefreshToken")),
            ).andExpect {
                status().isOk()
                jsonPath("$.message").value("LOGOUT_SUCCESS")
            }
    }

    @Test
    @DisplayName("로그아웃 테스트 (Authentication is null)")
    fun testLogoutUnauthenticatedNoAuthentication() {
        mockMvc
            .perform(
                delete("/api/v1/user/token")
                    .with(SecurityMockMvcRequestPostProcessors.anonymous()),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("UNAUTHORIZED"))
    }

    @Test
    @DisplayName("로그아웃 테스트 (Authentication principal is not User)")
    fun testLogoutUnauthenticatedInvalidPrincipal() {
        val auth =
            UsernamePasswordAuthenticationToken(
                "notJWTUser",
                null,
                emptyList(),
            )
        mockMvc
            .perform(
                delete("/api/v1/user/token")
                    .with(SecurityMockMvcRequestPostProcessors.authentication(auth)),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("UNAUTHORIZED"))
    }

    @Test
    @DisplayName("로그아웃 테스트 (인증되지 않은 사용자)")
    fun testLogoutUnauthenticated() {
        mockMvc
            .perform(
                delete("/api/v1/user/token")
                    .cookie(Cookie("access_token", "mockAccessToken"))
                    .cookie(Cookie("refresh_token", "mockRefreshToken")),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("UNAUTHORIZED"))
    }

    @Test
    @DisplayName("사용자 정보 조회 테스트 (정상)")
    @WithCustomMockUser(username = "test_user")
    fun testGetUserInfo() {
        whenever(authService.getUserInfo("test_user"))
            .thenReturn(
                User(
                    userID = "test_user",
                    password = "password".toByteArray(),
                    name = "Test User",
                    email = "test@example.com",
                    phone = "1234567890",
                    active = true,
                    permissions = mutableSetOf(AdminPermission.SUPER_ADMIN),
                ),
            )
        mockMvc
            .perform(
                get("/api/v1/user/profile"),
            ).andExpect {
                status().isOk()
                jsonPath("$.username").value("test_user")
                jsonPath("$.nickname").value("Test User")
                jsonPath("$.email").value("test@example.com")
                jsonPath("$.phone").value("1234567890")
                jsonPath("$.active").value(true)
                jsonPath("$.permissions[0]").value("SUPER_ADMIN")
                jsonPath("$.permissions[8]").value("NOTICE")
            }
    }

    @Test
    @DisplayName("사용자 정보 조회 테스트 (존재하지 않는 사용자)")
    @WithCustomMockUser(username = "non_existent_user")
    fun testGetUserInfoNonExistent() {
        doThrow(IllegalArgumentException("NO_USER_INFO"))
            .whenever(authService)
            .getUserInfo("non_existent_user")
        mockMvc
            .perform(
                get("/api/v1/user/profile"),
            ).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("NO_USER_INFO"))
    }

    @Test
    @DisplayName("사용자 정보 조회 테스트 (Authentication principal is not User)")
    fun testGetUserInfoUnauthenticatedInvalidPrincipal() {
        val auth =
            UsernamePasswordAuthenticationToken(
                "notJWTUser",
                null,
                emptyList(),
            )
        mockMvc
            .perform(
                get("/api/v1/user/profile")
                    .with(SecurityMockMvcRequestPostProcessors.authentication(auth)),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("UNAUTHORIZED"))
    }

    @Test
    @WithCustomMockUser(username = "test_user")
    fun authenticatedUserCanUpdateProfile() {
        val request = UpdateProfileRequest("Updated User", "updated@example.com", "01012345678")
        val user =
            User(
                "test_user",
                "password".toByteArray(),
                request.nickname,
                request.email,
                request.phone,
                true,
            )
        whenever(authService.updateProfile("test_user", request)).thenReturn(user)

        mockMvc
            .perform(
                patch("/api/v1/user/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.nickname").value("Updated User"))
    }

    @Test
    fun directProfileUpdateRejectsMissingSecurityContext() {
        SecurityContextHolder.clearContext()

        assertThrows<RuntimeException> {
            controller.updateProfile(UpdateProfileRequest("User", "user@example.com", ""))
        }
    }

    @Test
    @WithCustomMockUser(username = "test_user")
    fun updateProfileMapsDuplicateAndInvalidInput() {
        val request = UpdateProfileRequest("Updated User", "updated@example.com", "")
        doThrow(DuplicateEmailException()).whenever(authService).updateProfile("test_user", request)
        performProfileUpdate(request).andExpect(status().isConflict)

        doThrow(InvalidUserInputException("INVALID_USER_INPUT"))
            .whenever(authService)
            .updateProfile("test_user", request)
        performProfileUpdate(request)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("INVALID_USER_INPUT"))
    }

    @Test
    @WithCustomMockUser(username = "test_user")
    fun authenticatedUserCanChangePasswordAndErrorsAreMapped() {
        val request = ChangePasswordRequest("current", "Password1!")
        mockMvc
            .perform(
                put("/api/v1/user/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("PASSWORD_CHANGED"))
            .andExpect(cookie().maxAge("access_token", 0))
            .andExpect(cookie().maxAge("refresh_token", 0))

        doThrow(BadCredentialsException("CURRENT_PASSWORD_MISMATCH"))
            .whenever(authService)
            .changePassword("test_user", request)
        performPasswordChange(request)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("CURRENT_PASSWORD_MISMATCH"))

        doThrow(InvalidUserInputException("INVALID_PASSWORD"))
            .whenever(authService)
            .changePassword("test_user", request)
        performPasswordChange(request)
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("INVALID_PASSWORD"))
    }

    private fun performProfileUpdate(request: UpdateProfileRequest) =
        mockMvc.perform(
            patch("/api/v1/user/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )

    private fun performPasswordChange(request: ChangePasswordRequest) =
        mockMvc.perform(
            put("/api/v1/user/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
}
