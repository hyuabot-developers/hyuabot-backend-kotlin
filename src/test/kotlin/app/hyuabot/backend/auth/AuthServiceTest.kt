package app.hyuabot.backend.auth

import app.hyuabot.backend.auth.domain.ChangePasswordRequest
import app.hyuabot.backend.auth.domain.UpdateProfileRequest
import app.hyuabot.backend.auth.exception.DuplicateEmailException
import app.hyuabot.backend.auth.exception.InvalidUserInputException
import app.hyuabot.backend.database.entity.RefreshToken
import app.hyuabot.backend.database.entity.User
import app.hyuabot.backend.database.repository.RefreshTokenRepository
import app.hyuabot.backend.database.repository.UserRepository
import app.hyuabot.backend.security.JWTTokenProvider
import app.hyuabot.backend.security.JWTUser
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {
    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var refreshTokenRepository: RefreshTokenRepository

    @Mock
    lateinit var passwordEncoder: PasswordEncoder

    @Mock
    lateinit var authenticationManagerBuilder: AuthenticationManagerBuilder

    @Mock
    lateinit var request: HttpServletRequest

    @Mock
    lateinit var tokenProvider: JWTTokenProvider

    @InjectMocks
    lateinit var authService: AuthService

    private fun user(
        userID: String = "testUser",
        password: ByteArray? = "encoded-password".toByteArray(),
    ) = User(
        userID = userID,
        password = password,
        name = "Test User",
        email = "test@example.com",
        phone = "01000000000",
        active = true,
    )

    @Test
    @DisplayName("로그인 테스트")
    fun loginTest() {
        val userID = "testUser"
        val password = "testPassword"
        val authenticationToken = UsernamePasswordAuthenticationToken(userID, password)
        val authentication = mock<Authentication>()
        whenever(authenticationManagerBuilder.`object`).thenReturn(mock())
        whenever(authenticationManagerBuilder.`object`.authenticate(authenticationToken)).thenReturn(authentication)
        whenever(tokenProvider.createAccessToken(authentication)).thenReturn("accessToken")
        whenever(tokenProvider.createRefreshToken(authentication)).thenReturn("refreshToken")

        val tokenResponse = authService.login(userID, password)
        assertEquals("Bearer", tokenResponse.grantType)
        assertEquals("accessToken", tokenResponse.accessToken)
        assertEquals("refreshToken", tokenResponse.refreshToken)
    }

    @Test
    @DisplayName("토큰 갱신 테스트")
    fun refreshTokenTest() {
        val authentication = mock<Authentication>()
        val savedRefreshToken = mock<app.hyuabot.backend.database.entity.RefreshToken>()
        whenever(authentication.principal).thenReturn(JWTUser("testUser", ""))
        whenever(tokenProvider.getAuthentication("refreshToken")).thenReturn(authentication)
        whenever(savedRefreshToken.refreshToken).thenReturn("refreshToken")
        whenever(refreshTokenRepository.findByUserID("testUser")).thenReturn(savedRefreshToken)
        whenever(tokenProvider.createAccessToken(authentication)).thenReturn("newAccessToken")
        assertEquals("newAccessToken", authService.refreshToken("refreshToken"))
    }

    @Test
    fun refreshTokenRejectsRevokedToken() {
        val authentication = mock<Authentication>()
        whenever(authentication.principal).thenReturn(JWTUser("testUser", ""))
        whenever(tokenProvider.getAuthentication("refreshToken")).thenReturn(authentication)
        whenever(refreshTokenRepository.findByUserID("testUser")).thenReturn(null)

        assertThrows<BadCredentialsException> { authService.refreshToken("refreshToken") }
    }

    @Test
    @DisplayName("사용자 정보 조회 테스트")
    fun getUserInfoTest() {
        val userID = "testUser"
        val user = mock<User>()
        whenever(userRepository.findByUserIDAndActiveIsTrue(userID)).thenReturn(user)
        assertEquals(user, authService.getUserInfo(userID))
    }

    @Test
    @DisplayName("사용자 정보 조회 테스트 (존재하지 않는 사용자)")
    fun getUserInfoNotFoundTest() {
        val userID = "nonExistentUser"
        whenever(userRepository.findByUserIDAndActiveIsTrue(userID)).thenReturn(null)
        assertThrows<IllegalArgumentException> { authService.getUserInfo(userID) }
    }

    @Test
    fun updateProfileNormalizesAndSavesUser() {
        val user = user()
        whenever(userRepository.findByUserIDAndActiveIsTrue("testUser")).thenReturn(user)
        whenever(userRepository.save(user)).thenReturn(user)

        val result =
            authService.updateProfile(
                "testUser",
                UpdateProfileRequest(" Updated User ", " UPDATED@EXAMPLE.COM ", " 01012345678 "),
            )

        assertEquals("Updated User", result.name)
        assertEquals("updated@example.com", result.email)
        assertEquals("01012345678", result.phone)
        verify(userRepository).save(user)
    }

    @Test
    fun updateProfileAllowsSameEmailAndRejectsAnotherUsersEmail() {
        val user = user()
        whenever(userRepository.findByUserIDAndActiveIsTrue("testUser")).thenReturn(user)
        whenever(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(user)
        whenever(userRepository.save(user)).thenReturn(user)
        authService.updateProfile("testUser", UpdateProfileRequest("User", "test@example.com", ""))

        whenever(userRepository.findByEmailIgnoreCase("other@example.com")).thenReturn(user("other"))
        assertThrows<DuplicateEmailException> {
            authService.updateProfile("testUser", UpdateProfileRequest("User", "other@example.com", ""))
        }
    }

    @Test
    fun updateProfileRejectsInvalidInput() {
        whenever(userRepository.findByUserIDAndActiveIsTrue("testUser")).thenReturn(user())
        assertThrows<InvalidUserInputException> {
            authService.updateProfile("testUser", UpdateProfileRequest(" ", "test@example.com", ""))
        }
        assertThrows<InvalidUserInputException> {
            authService.updateProfile("testUser", UpdateProfileRequest("User", " ", ""))
        }
    }

    @Test
    fun changePasswordRevokesRefreshTokenAndIncrementsAuthVersion() {
        val user = user()
        val refreshToken = mock<RefreshToken>()
        whenever(userRepository.findByUserIDAndActiveIsTrue("testUser")).thenReturn(user)
        whenever(passwordEncoder.matches("current", "encoded-password")).thenReturn(true)
        whenever(passwordEncoder.encode("a-new-secure-password")).thenReturn("new-encoded")
        whenever(refreshTokenRepository.findByUserID("testUser")).thenReturn(refreshToken)

        authService.changePassword("testUser", ChangePasswordRequest("current", "a-new-secure-password"))

        assertEquals("new-encoded", user.password?.decodeToString())
        assertEquals(1, user.authVersion)
        verify(userRepository).save(user)
        verify(refreshTokenRepository).delete(refreshToken)
    }

    @Test
    fun changePasswordSucceedsWithoutExistingRefreshToken() {
        val user = user()
        whenever(userRepository.findByUserIDAndActiveIsTrue("testUser")).thenReturn(user)
        whenever(passwordEncoder.matches("current", "encoded-password")).thenReturn(true)
        whenever(passwordEncoder.encode("a-new-secure-password")).thenReturn("new-encoded")
        whenever(refreshTokenRepository.findByUserID("testUser")).thenReturn(null)

        authService.changePassword("testUser", ChangePasswordRequest("current", "a-new-secure-password"))

        assertEquals("new-encoded", user.password?.decodeToString())
        assertEquals(1, user.authVersion)
    }

    @Test
    fun changePasswordRejectsWrongMissingOrInvalidPassword() {
        val user = user()
        whenever(userRepository.findByUserIDAndActiveIsTrue("testUser")).thenReturn(user)
        whenever(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false)
        assertThrows<BadCredentialsException> {
            authService.changePassword("testUser", ChangePasswordRequest("wrong", "a-new-secure-password"))
        }

        whenever(userRepository.findByUserIDAndActiveIsTrue("testUser")).thenReturn(user(password = null))
        assertThrows<BadCredentialsException> {
            authService.changePassword("testUser", ChangePasswordRequest("current", "a-new-secure-password"))
        }

        assertThrows<InvalidUserInputException> {
            authService.changePassword("testUser", ChangePasswordRequest("current", "short"))
        }
    }

    @Test
    fun changePasswordRejectsEncoderFailure() {
        val user = user()
        whenever(userRepository.findByUserIDAndActiveIsTrue("testUser")).thenReturn(user)
        whenever(passwordEncoder.matches("current", "encoded-password")).thenReturn(true)
        whenever(passwordEncoder.encode("a-new-secure-password")).thenReturn(null)

        assertThrows<InvalidUserInputException> {
            authService.changePassword("testUser", ChangePasswordRequest("current", "a-new-secure-password"))
        }
        assertTrue(user.authVersion == 0)
    }

    @Test
    @DisplayName("로그아웃 테스트")
    fun logoutTest() {
        val user =
            mock<User>().apply {
                whenever(userID).thenReturn("testUser")
            }
        val accessToken = "accessToken"
        val cookies = arrayOf(Cookie("access_token", accessToken))
        whenever(request.cookies).thenReturn(cookies)
        whenever(refreshTokenRepository.findByUserID(user.userID)).thenReturn(mock())
        authService.logout(user, request)
        verify(tokenProvider).invalidateAccessToken(user, accessToken)
        verify(refreshTokenRepository).delete(any())
    }

    @Test
    @DisplayName("로그아웃 테스트 (Cookie 없음)")
    fun logoutNoCookieTest() {
        val user = mock<User>()
        val cookies = arrayOf(Cookie("access_token", null))
        whenever(request.cookies).thenReturn(cookies)
        val exception = assertThrows<IllegalArgumentException> { authService.logout(user, request) }
        assertEquals("NO_ACCESS_TOKEN", exception.message)
    }

    @Test
    @DisplayName("로그아웃 테스트 (Access Token 없음)")
    fun logoutNoAccessTokenTest() {
        val user = mock<User>()
        whenever(request.cookies).thenReturn(emptyArray())
        val exception = assertThrows<IllegalArgumentException> { authService.logout(user, request) }
        assertEquals("NO_ACCESS_TOKEN", exception.message)
    }

    @Test
    @DisplayName("로그아웃 테스트 (Refresh Token 없음)")
    fun logoutNoRefreshTokenTest() {
        val user =
            mock<User>().apply {
                whenever(userID).thenReturn("testUser")
            }
        val cookies = arrayOf(Cookie("access_token", "accessToken"))
        whenever(request.cookies).thenReturn(cookies)
        whenever(refreshTokenRepository.findByUserID(user.userID)).thenReturn(null)
        val exception = assertThrows<IllegalArgumentException> { authService.logout(user, request) }
        assertEquals("NO_REFRESH_TOKEN", exception.message)
    }
}
