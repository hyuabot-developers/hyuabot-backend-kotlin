package app.hyuabot.backend.auth

import app.hyuabot.backend.auth.domain.CreateUserRequest
import app.hyuabot.backend.auth.exception.DuplicateEmailException
import app.hyuabot.backend.auth.exception.DuplicateUserIDException
import app.hyuabot.backend.database.entity.User
import app.hyuabot.backend.database.repository.RefreshTokenRepository
import app.hyuabot.backend.database.repository.UserRepository
import app.hyuabot.backend.security.JWTTokenProvider
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
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.assertEquals

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

    @Test
    @DisplayName("회원가입 테스트")
    fun signUpTest() {
        val payload =
            CreateUserRequest(
                userID = "testUser",
                password = "testPassword",
                nickname = "Test User",
                email = "test@example.com",
                phone = "1234567890",
            )
        whenever(userRepository.findByUserID(payload.userID)).thenReturn(null)
        whenever(userRepository.findByEmail(payload.email)).thenReturn(null)
        whenever(passwordEncoder.encode(payload.password)).thenReturn("hashedPassword")
        authService.signUp(payload)
        verify(userRepository).save(
            argThat {
                userID == payload.userID &&
                    String(password) == "hashedPassword" &&
                    name == payload.nickname &&
                    email == payload.email &&
                    phone == payload.phone &&
                    !active
            },
        )
    }

    @Test
    @DisplayName("회원가입 테스트 (중복 사용자 이름)")
    fun signUpDuplicateUserIDTest() {
        val payload =
            CreateUserRequest(
                userID = "testUser",
                password = "testPassword",
                nickname = "Test User",
                email = "test@example.com",
                phone = "1234567890",
            )
        whenever(userRepository.findByUserID(payload.userID)).thenReturn(mock())
        assertThrows<DuplicateUserIDException> { authService.signUp(payload) }
    }

    @Test
    @DisplayName("회원가입 테스트 (중복 이메일)")
    fun signUpDuplicateEmailTest() {
        val payload =
            CreateUserRequest(
                userID = "testUser",
                password = "testPassword",
                nickname = "Test User",
                email = "test@example.com",
                phone = "1234567890",
            )
        whenever(userRepository.findByUserID(payload.userID)).thenReturn(null)
        whenever(userRepository.findByEmail(payload.email)).thenReturn(mock())
        assertThrows<DuplicateEmailException> { authService.signUp(payload) }
    }

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
        whenever(tokenProvider.getAuthentication("refreshToken")).thenReturn(authentication)
        whenever(tokenProvider.createRefreshToken(authentication)).thenReturn("newRefreshToken")
        assertEquals("newRefreshToken", authService.refreshToken("refreshToken"))
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
