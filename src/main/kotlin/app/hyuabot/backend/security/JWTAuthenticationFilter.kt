package app.hyuabot.backend.security

import app.hyuabot.backend.auth.domain.TokenResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Order(0)
@Component
class JWTAuthenticationFilter(
    private val tokenProvider: JWTTokenProvider,
    private val redisTemplate: RedisTemplate<String, String>,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        resolveToken(request)?.let { token ->
            // 로그아웃 여부 확인
            if (checkLogout(token)) {
                request.setAttribute("error", "LOGOUT")
                return@let
            }
            responseHandler(request, response) {
                (tokenProvider.getAuthentication(token) to token)
            }
        }
        filterChain.doFilter(request, response)
    }

    // Authentication 헤더에서 JWT 토큰을 추출
    private fun resolveToken(request: HttpServletRequest): String? =
        request
            .getHeader("Authorization")
            ?.takeIf {
                it.startsWith(TokenResponse.TokenGrantType.BEARER.value)
            }?.substring(7)

    // 인증 정보를 기반으로 응답 헤더에 JWT 토큰을 설정
    private fun responseHandler(
        request: HttpServletRequest,
        response: HttpServletResponse,
        resolveAuthInfo: () -> Pair<Authentication, String>,
    ) = try {
        val (authentication, token) = resolveAuthInfo()
        SecurityContextHolder.getContext().authentication = authentication
        response.setHeader("Authorization", "Bearer $token")
    } catch (e: Exception) {
        request.setAttribute("error", e.message)
    }

    // Logout 여부 확인
    private fun checkLogout(accessToken: String): Boolean {
        redisTemplate.opsForValue().get("access_token:$accessToken")?.let {
            if (it == "logout") {
                return true
            }
        }
        return false
    }
}
