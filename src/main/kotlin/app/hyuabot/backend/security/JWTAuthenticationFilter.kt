package app.hyuabot.backend.security

import app.hyuabot.backend.auth.domain.TokenResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Order(0)
@Component
class JWTAuthenticationFilter(
    private val tokenProvider: JWTTokenProvider,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        resolveToken(request)?.let { token ->
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
}
