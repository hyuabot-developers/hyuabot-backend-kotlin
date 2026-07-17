package app.hyuabot.backend.security

import app.hyuabot.backend.database.entity.RefreshToken
import app.hyuabot.backend.database.repository.RefreshTokenRepository
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Date
import java.util.UUID

@Component
class JWTTokenProvider(
    @param:Value("\${jwt.secret}") private val secret: String,
    @param:Value("\${jwt.expiration}") private val expirationMinutes: Long,
    @param:Value("\${jwt.expiration.refresh}") private val refreshExpirationDays: Long,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val userDetailsService: JWTUserDetailsService,
) {
    private val key by lazy { Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)) }

    // 기존 Access token 무효화
    fun invalidateAccessToken(accessToken: String) {
        redisTemplate.opsForValue().set(
            "access_token:$accessToken",
            "logout",
            Duration.ofMinutes(expirationMinutes),
        )
    }

    // Access token 생성
    fun createAccessToken(authentication: Authentication): String =
        Jwts
            .builder()
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationMinutes * 1000 * 60)) // 만료 시간 설정
            .subject((authentication.principal as JWTUser).username)
            .claim(AUTH_VERSION_CLAIM, (authentication.principal as JWTUser).authVersion)
            .signWith(key)
            .compact()

    fun createAccessToken(userID: String): String =
        Jwts
            .builder()
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationMinutes * 1000 * 60)) // 만료 시간 설정
            .subject(userID)
            .claim(AUTH_VERSION_CLAIM, 0)
            .signWith(key)
            .compact()

    // Refresh token 생성
    fun createRefreshToken(authentication: Authentication): String {
        val userID = (authentication.principal as JWTUser).username
        val sessionID = UUID.randomUUID()
        val now = ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone)
        val refreshToken =
            Jwts
                .builder()
                .id(sessionID.toString())
                .issuedAt(Date())
                .expiration(Date(System.currentTimeMillis() + refreshExpirationDays * 1000 * 60 * 60 * 24)) // 만료 시간 설정
                .subject(userID)
                .claim(AUTH_VERSION_CLAIM, (authentication.principal as JWTUser).authVersion)
                .signWith(key)
                .compact()
        refreshTokenRepository.save(
            RefreshToken(
                uuid = sessionID,
                userID = userID,
                refreshToken = refreshToken,
                expiredAt = now.plusDays(refreshExpirationDays),
                createdAt = now,
                updatedAt = now,
                user = null,
            ),
        )
        return refreshToken
    }

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
        val principal: UserDetails = userDetailsService.loadUserByUsername(claims.subject)
        val tokenAuthVersion = (claims[AUTH_VERSION_CLAIM] as? Number)?.toInt()
        if (principal !is JWTUser || tokenAuthVersion != principal.authVersion) {
            throw BadCredentialsException("TOKEN_REVOKED")
        }
        return UsernamePasswordAuthenticationToken(principal, "", principal.authorities)
    }

    companion object {
        private const val AUTH_VERSION_CLAIM = "auth_version"
    }
}
