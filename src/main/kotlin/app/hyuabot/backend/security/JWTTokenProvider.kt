package app.hyuabot.backend.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.util.Date

@Component
class JWTTokenProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration}") private val expiration: Long,
) {
    private val key by lazy { Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)) }

    // Access token 생성
    fun createAccessToken(authentication: Authentication): String =
        Jwts
            .builder()
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expiration * 1000))
            .subject((authentication.principal as JWTUser).username)
            .signWith(key)
            .compact()

    // 토큰 검증 및 정보 추출
    fun getAuthentication(token: String): Authentication {
        val claims: Claims = getClaimsWithValidation(token)
        return getAuthentication(claims)
    }

    // 토큰에서 Claims 추출
    private fun getClaimsWithValidation(token: String): Claims =
        Jwts
            .parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

    // Claims 에서 Authentication 객체 생성
    private fun getAuthentication(claims: Claims): Authentication {
        val principal: UserDetails =
            JWTUser(
                username = claims.subject,
                password = "",
            )
        return UsernamePasswordAuthenticationToken(principal, "", principal.authorities)
    }
}
