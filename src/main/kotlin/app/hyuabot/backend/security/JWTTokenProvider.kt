package app.hyuabot.backend.security

import app.hyuabot.backend.database.entity.RefreshToken
import app.hyuabot.backend.database.entity.User
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
    fun invalidateAccessToken(
        user: User,
        accessToken: String,
    ) {
        refreshTokenRepository.findByUserID(user.userID)?.let {
            redisTemplate.opsForValue().set(
                "access_token:$accessToken",
                "logout",
                Duration.between(
                    ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone),
                    it.expiredAt,
                ),
            )
        }
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
        val refreshToken =
            Jwts
                .builder()
                .issuedAt(Date())
                .expiration(Date(System.currentTimeMillis() + refreshExpirationDays * 1000 * 60 * 60 * 24)) // 만료 시간 설정
                .subject(userID)
                .claim(AUTH_VERSION_CLAIM, (authentication.principal as JWTUser).authVersion)
                .signWith(key)
                .compact()
        val previousRefreshToken = refreshTokenRepository.findByUserID(userID)
        if (previousRefreshToken != null) {
            previousRefreshToken.apply {
                this.refreshToken = refreshToken
                this.expiredAt = ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone).plusDays(refreshExpirationDays)
                this.updatedAt = ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone)
            }
            refreshTokenRepository.save(previousRefreshToken)
        } else {
            refreshTokenRepository.save(
                RefreshToken(
                    uuid = UUID.randomUUID(),
                    userID = userID,
                    refreshToken = refreshToken,
                    expiredAt = ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone).plusDays(refreshExpirationDays),
                    createdAt = ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone),
                    updatedAt = ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone),
                    user = null,
                ),
            )
        }
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
