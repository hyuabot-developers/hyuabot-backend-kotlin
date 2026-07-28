package app.hyuabot.backend.security

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthCookieProviderTest {
    private val refreshExpirationDays = 7L
    private val provider = AuthCookieProvider(refreshExpirationDays)

    @Test
    @DisplayName("access token 쿠키는 refresh 유효기간과 동일한 Max-Age 를 가진 영속 쿠키로 발급된다")
    fun accessTokenCookieIsPersistent() {
        val cookie = provider.accessTokenCookie("access-value")
        assertEquals("access_token", cookie.name)
        assertEquals("access-value", cookie.value)
        assertEquals(Duration.ofDays(refreshExpirationDays), cookie.maxAge)
        assertTrue(cookie.isHttpOnly)
        assertTrue(cookie.isSecure)
        assertEquals("None", cookie.sameSite)
        assertEquals("/", cookie.path)
    }

    @Test
    @DisplayName("refresh token 쿠키는 refresh 유효기간과 동일한 Max-Age 를 가진 영속 쿠키로 발급된다")
    fun refreshTokenCookieIsPersistent() {
        val cookie = provider.refreshTokenCookie("refresh-value")
        assertEquals("refresh_token", cookie.name)
        assertEquals("refresh-value", cookie.value)
        assertEquals(Duration.ofDays(refreshExpirationDays), cookie.maxAge)
    }

    @Test
    @DisplayName("만료 쿠키는 access/refresh 두 개가 Max-Age 0 으로 발급된다")
    fun expiredAuthCookiesHaveZeroMaxAge() {
        val cookies = provider.expiredAuthCookies()
        assertEquals(2, cookies.size)
        assertEquals(listOf("access_token", "refresh_token"), cookies.map { it.name })
        assertTrue(cookies.all { it.maxAge == Duration.ZERO })
        assertTrue(cookies.all { it.value.isEmpty() })
    }
}
