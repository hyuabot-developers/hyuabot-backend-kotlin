package app.hyuabot.backend.auth

import app.hyuabot.backend.auth.domain.TokenResponse
import app.hyuabot.backend.auth.exception.DuplicateEmailException
import app.hyuabot.backend.auth.exception.DuplicateUserIDException
import app.hyuabot.backend.database.entity.User
import app.hyuabot.backend.database.repository.UserRepository
import app.hyuabot.backend.security.WithCustomMockUser
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {
    @MockitoBean
    private lateinit var authService: AuthService

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
    @DisplayName("회원가입 테스트 (정상)")
    fun testSignUp() {
        val map: Map<String, String> =
            mapOf(
                "userID" to "new_user",
                "nickname" to "New User",
                "password" to "new_password",
                "email" to "new@example.com",
                "phone" to "0987654321",
            )
        mockMvc
            .perform(
                post("/api/v1/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(map)),
            ).andExpect {
                status().isCreated()
                jsonPath("$.message").value("USER_CREATED_SUCCESSFULLY")
            }
    }

    @Test
    @DisplayName("회원가입 테스트 (이미 존재하는 사용자 ID)")
    fun testSignUpDuplicateUserID() {
        doThrow(DuplicateUserIDException::class)
            .whenever(authService)
            .signUp(any())
        val map: Map<String, String> =
            mapOf(
                "userID" to "test_user",
                "nickname" to "Duplicate User",
                "password" to "duplicate_password",
                "email" to "new@example.com",
                "phone" to "0987654321",
            )
        mockMvc
            .perform(
                post("/api/v1/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(map)),
            ).andExpect {
                status().isConflict()
                jsonPath("$.message").value("DUPLICATE_USER_ID")
            }
    }

    @Test
    @DisplayName("회원가입 테스트 (이미 존재하는 이메일)")
    fun testSignUpDuplicateEmail() {
        doThrow(DuplicateEmailException::class)
            .whenever(authService)
            .signUp(any())
        val map: Map<String, String> =
            mapOf(
                "userID" to "another_user",
                "nickname" to "Another User",
                "password" to "another_password",
                "email" to "test@example.com",
                "phone" to "0987654321",
            )
        mockMvc
            .perform(
                post("/api/v1/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(map)),
            ).andExpect {
                status().isConflict()
                jsonPath("$.message").value("DUPLICATE_EMAIL")
            }
    }

    @Test
    @DisplayName("회원가입 테스트 (오류)")
    fun testSignUpError() {
        doThrow(RuntimeException("DB_ERROR"))
            .whenever(authService)
            .signUp(any())
        val map: Map<String, String> =
            mapOf(
                "userID" to "another_user",
                "nickname" to "Another User",
                "password" to "another_password",
                "email" to "another@example.com",
                "phone" to "0987654321",
            )
        mockMvc
            .perform(
                post("/api/v1/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(map)),
            ).andExpect {
                status().isInternalServerError()
                jsonPath("$.message").value("INTERNAL_SERVER_ERROR")
            }
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
            ).andExpect {
                status().isCreated()
                cookie().exists("access_token")
                cookie().exists("refresh_token")
                jsonPath("$.message").value("LOGIN_SUCCESS")
            }
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
            ).andExpect {
                status().isUnauthorized()
                jsonPath("$.message").value("INVALID_CREDENTIALS")
            }
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
            ).andExpect {
                status().isInternalServerError()
                jsonPath("$.message").value("INTERNAL_SERVER_ERROR")
            }
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
            ).andExpect {
                status().isOk()
                cookie().exists("access_token")
                jsonPath("$.message").value("TOKEN_REFRESH_SUCCESS")
            }
    }

    @Test
    @DisplayName("토큰 갱신 테스트 (토큰 없음)")
    fun testRefreshTokenWithoutCookie() {
        mockMvc
            .perform(
                put("/api/v1/user/token"),
            ).andExpect {
                status().isUnauthorized()
                jsonPath("$.message").value("UNAUTHORIZED")
            }
    }

    @Test
    @DisplayName("토큰 갱신 테스트 (Refresh Token 없음)")
    fun testRefreshTokenWithoutRefreshToken() {
        mockMvc
            .perform(
                put("/api/v1/user/token")
                    .cookie(Cookie("test", "testCookie")),
            ).andExpect {
                status().isUnauthorized()
                jsonPath("$.message").value("UNAUTHORIZED")
            }
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
    @DisplayName("로그아웃 테스트 (인증되지 않은 사용자)")
    fun testLogoutUnauthenticated() {
        mockMvc
            .perform(
                delete("/api/v1/user/token")
                    .cookie(Cookie("access_token", "mockAccessToken"))
                    .cookie(Cookie("refresh_token", "mockRefreshToken")),
            ).andExpect {
                status().isUnauthorized()
                jsonPath("$.message").value("UNAUTHORIZED")
            }
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
            ).andExpect {
                status().isNotFound()
                jsonPath("$.message").value("NO_USER_INFO")
            }
    }
}
