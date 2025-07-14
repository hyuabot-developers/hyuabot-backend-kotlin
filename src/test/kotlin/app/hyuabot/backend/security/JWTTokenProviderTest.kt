package app.hyuabot.backend.security

import app.hyuabot.backend.database.entity.RefreshToken
import app.hyuabot.backend.database.entity.User
import app.hyuabot.backend.database.repository.RefreshTokenRepository
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.startsWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.security.core.Authentication
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Base64
import java.util.UUID

class JWTTokenProviderTest {
    // Test Target: JWTTokenProvider
    private lateinit var jwtTokenProvider: JWTTokenProvider

    // Constants for testing
    private val jwtSource = "testJWTSecretPassword1234567890!@#$%^&*()_+"
    private val secret = Base64.getEncoder().encodeToString(jwtSource.toByteArray())
    private val expirationMinutes = 60L
    private val refreshExpirationDays = 30L

    // Mock dependencies
    @Mock
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Mock
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @Mock
    private lateinit var valueOperations: ValueOperations<String, String>

    @Mock
    private lateinit var authentication: Authentication

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        given(redisTemplate.opsForValue()).willReturn(valueOperations)
        // Initialize JWTTokenProvider with mocked dependencies
        jwtTokenProvider =
            JWTTokenProvider(
                secret = secret,
                expirationMinutes = expirationMinutes,
                refreshExpirationDays = refreshExpirationDays,
                refreshTokenRepository = refreshTokenRepository,
                redisTemplate = redisTemplate,
            )
        val jwtUser = JWTUser("testUser", "")
        given(authentication.principal).willReturn(jwtUser)
    }

    @Test
    fun testCreateAccessTokenWithUserID() {
        val token = jwtTokenProvider.createAccessToken("testUser")
        assert(token.isNotBlank())
        // Check claims in the token
        val claims = parseClaims(token)
        assert(claims.subject == "testUser")
    }

    @Test
    fun testCreateAccessTokenWithAuthentication() {
        val token = jwtTokenProvider.createAccessToken(authentication)
        assert(token.isNotBlank())
        // Check claims in the token
        val claims = parseClaims(token)
        assert(claims.subject == "testUser")
    }

    @Test
    fun testCreateRefreshToken() {
        // Check if the refresh token is not stored in the repository
        given(refreshTokenRepository.findByUserID("testUser")).willReturn(null)
        val token = jwtTokenProvider.createRefreshToken(authentication)
        assert(token.isNotBlank())
        then(refreshTokenRepository).should().findByUserID("testUser")
    }

    @Test
    fun testExtendRefreshToken() {
        // Mock existing refresh token
        val now = ZonedDateTime.now()
        val existingToken =
            RefreshToken(
                uuid = UUID.randomUUID(),
                userID = "testUser",
                refreshToken = "previousRefreshToken",
                expiredAt = now.plusDays(1),
                createdAt = now.minusDays(10),
                updatedAt = now.minusDays(1),
                user = null,
            )
        given(refreshTokenRepository.findByUserID("testUser")).willReturn(existingToken)
        // Extend the refresh token
        val newToken = jwtTokenProvider.createRefreshToken(authentication)
        assert(newToken.isNotBlank())
        assert(existingToken.refreshToken == newToken)
        assert(existingToken.expiredAt.isAfter(now))
        assert(existingToken.updatedAt.isAfter(now.minusDays(1)))
        then(refreshTokenRepository).should().save(existingToken)
    }

    @Test
    fun testGetAuthentication() {
        val token = jwtTokenProvider.createAccessToken(authentication)
        val auth = jwtTokenProvider.getAuthentication(token)
        assert(auth.isAuthenticated)
        assert((auth.principal as JWTUser).username == "testUser")
    }

    @Test
    fun testInvalidateAccessToken() {
        // Mock existing refresh token
        val now = ZonedDateTime.now()
        val existingToken =
            RefreshToken(
                uuid = UUID.randomUUID(),
                userID = "testUser",
                refreshToken = "previousRefreshToken",
                expiredAt = now.plusDays(1),
                createdAt = now.minusDays(10),
                updatedAt = now.minusDays(1),
                user = null,
            )
        given(refreshTokenRepository.findByUserID("testUser")).willReturn(existingToken)
        // Invalidate the access token
        val user =
            User(
                userID = "testUser",
                password = ByteArray(0),
                name = "Test User",
                email = "",
                phone = "",
                active = true,
            )
        val accessToken = jwtTokenProvider.createAccessToken("testUser")
        jwtTokenProvider.invalidateAccessToken(user, accessToken)
        then(valueOperations).should().set(
            startsWith("access_token:"),
            eq("logout"),
            any<Duration>(),
        )
    }

    @Test
    fun testInvalidAccessTokenWithoutRefreshToken() {
        // Mock no existing refresh token
        given(refreshTokenRepository.findByUserID("testUser")).willReturn(null)
        // Attempt to invalidate access token without a refresh token
        val user =
            User(
                userID = "testUser",
                password = ByteArray(0),
                name = "Test User",
                email = "",
                phone = "",
                active = true,
            )
        val accessToken = jwtTokenProvider.createAccessToken("testUser")
        jwtTokenProvider.invalidateAccessToken(user, accessToken)
        // Verify that no interaction with redisTemplate occurred
        then(valueOperations).shouldHaveNoInteractions()
    }

    fun parseClaims(token: String): Claims {
        val key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret))
        return Jwts
            .parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
