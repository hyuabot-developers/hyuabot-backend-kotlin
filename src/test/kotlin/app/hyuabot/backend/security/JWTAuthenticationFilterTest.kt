package app.hyuabot.backend.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(MockitoExtension::class)
class JWTAuthenticationFilterTest {
    @Mock
    lateinit var jwtTokenProvider: JWTTokenProvider

    @Mock
    lateinit var redisTemplate: RedisTemplate<String, String>

    @Mock
    lateinit var valueOperations: ValueOperations<String, String>

    @Mock
    lateinit var filterChain: FilterChain

    lateinit var jwtAuthenticationFilter: JWTAuthenticationFilter

    @BeforeEach
    fun setup() {
        jwtAuthenticationFilter = JWTAuthenticationFilter(jwtTokenProvider, redisTemplate)
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("토큰이 없을 때 필터가 정상적으로 동작하는지 확인")
    fun doFilterInternalWithoutToken() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        jwtAuthenticationFilter.doFilter(request, response, filterChain)
        assertNull(SecurityContextHolder.getContext().authentication)
        verify(filterChain).doFilter(request, response)
    }

    @Test
    @DisplayName("로그아웃된 토큰이 있을 때 필터가 정상적으로 동작하는지 확인")
    fun doFilterInternalWithLoggedOutToken() {
        val request =
            MockHttpServletRequest().apply {
                setCookies(Cookie("access_token", "logged_out_token"))
            }
        val response = MockHttpServletResponse()
        whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
        whenever(valueOperations.get("access_token:logged_out_token")).thenReturn("logout")
        jwtAuthenticationFilter.doFilter(request, response, filterChain)
        assertNull(SecurityContextHolder.getContext().authentication)
        assertEquals("LOGOUT", request.getAttribute("error"))
        verify(filterChain).doFilter(request, response)
    }

    @Test
    @DisplayName("유효한 토큰이 있을 때 필터가 정상적으로 동작하는지 확인")
    fun doFilterInternalWithValidToken() {
        val request =
            MockHttpServletRequest().apply {
                setCookies(Cookie("access_token", "valid_token"))
            }
        val response = MockHttpServletResponse()
        val jwtUser = JWTUser("testUser", "")
        val authentication = UsernamePasswordAuthenticationToken(jwtUser, null, jwtUser.authorities)
        whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
        whenever(valueOperations.get("access_token:valid_token")).thenReturn(null)
        whenever(jwtTokenProvider.getAuthentication("valid_token")).thenReturn(authentication)

        jwtAuthenticationFilter.doFilter(request, response, filterChain)

        val context = SecurityContextHolder.getContext()
        assertEquals(authentication, context.authentication)
        assertTrue(response.getHeader("Set-Cookie")?.contains("access_token=valid_token") ?: false)
        verify(filterChain).doFilter(request, response)
    }

    @Test
    @DisplayName("resolveToken 메서드가 쿠키에서 토큰을 올바르게 추출하는지 확인")
    fun resolveTokenExtractsTokenFromCookies() {
        val request =
            MockHttpServletRequest().apply {
                setCookies(Cookie("access_token", "extracted_token"))
            }
        val token = jwtAuthenticationFilter.resolveToken(request)
        assertEquals("extracted_token", token)
    }

    @Test
    @DisplayName("resolveToken 메서드가 쿠키가 없을 때 null을 반환하는지 확인")
    fun resolveTokenReturnsNullWhenNoCookies() {
        val request = MockHttpServletRequest()
        val token = jwtAuthenticationFilter.resolveToken(request)
        assertNull(token)
    }

    @Test
    @DisplayName("checkLogout 메서드가 로그아웃된 토큰을 올바르게 확인하는지 테스트")
    fun checkLogoutWithLoggedOutToken() {
        val accessToken = "logged_out_token"
        whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
        whenever(valueOperations.get("access_token:$accessToken")).thenReturn("logout")
        val isLoggedOut = jwtAuthenticationFilter.checkLogout(accessToken)
        assertTrue(isLoggedOut)
    }

    @Test
    @DisplayName("checkLogout 메서드가 Redis에 다른 값이 있을 때 false를 반환하는지 테스트")
    fun checkLogoutWithOtherValue() {
        val accessToken = "other_access_token"
        whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
        whenever(valueOperations.get("access_token:$accessToken")).thenReturn("some_other_value")
        val isLoggedOut = jwtAuthenticationFilter.checkLogout(accessToken)
        assertTrue(!isLoggedOut)
    }
}
