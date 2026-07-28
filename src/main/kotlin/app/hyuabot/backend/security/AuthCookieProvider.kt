package app.hyuabot.backend.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

// 인증 토큰 쿠키 생성기.
// iOS 홈 화면 PWA(standalone 모드)는 세션 쿠키를 앱 종료/메모리 회수 시 삭제하므로,
// refresh token 유효기간과 동일한 Max-Age 를 부여해 영속 쿠키로 발급한다.
@Component
class AuthCookieProvider(
    @param:Value("\${jwt.expiration.refresh}") private val refreshExpirationDays: Long,
) {
    fun accessTokenCookie(value: String): ResponseCookie = persistentCookie(ACCESS_TOKEN, value)

    fun refreshTokenCookie(value: String): ResponseCookie = persistentCookie(REFRESH_TOKEN, value)

    fun expiredAuthCookies(): List<ResponseCookie> = listOf(expiredCookie(ACCESS_TOKEN), expiredCookie(REFRESH_TOKEN))

    private fun persistentCookie(
        name: String,
        value: String,
    ): ResponseCookie =
        baseCookie(name, value)
            .maxAge(Duration.ofDays(refreshExpirationDays))
            .build()

    private fun expiredCookie(name: String): ResponseCookie =
        baseCookie(name, "")
            .maxAge(0)
            .build()

    private fun baseCookie(
        name: String,
        value: String,
    ) = ResponseCookie
        .from(name, value)
        .httpOnly(true)
        .secure(true)
        .sameSite("None")
        .path("/")

    companion object {
        private const val ACCESS_TOKEN = "access_token"
        private const val REFRESH_TOKEN = "refresh_token"
    }
}
