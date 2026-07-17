package app.hyuabot.backend.security

import app.hyuabot.backend.database.repository.RefreshTokenRepository
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.startsWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import java.time.Duration
import java.util.Base64
import org.springframework.security.core.userdetails.User as SpringUser

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

    @Mock
    private lateinit var userDetailsService: JWTUserDetailsService

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
                userDetailsService = userDetailsService,
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
        val token = jwtTokenProvider.createRefreshToken(authentication)
        val savedSession = argumentCaptor<app.hyuabot.backend.database.entity.RefreshToken>()

        assert(token.isNotBlank())
        then(refreshTokenRepository).should().save(savedSession.capture())
        assert(savedSession.firstValue.userID == "testUser")
        assert(savedSession.firstValue.refreshToken == token)
        assert(savedSession.firstValue.uuid.toString() == parseClaims(token).id)
    }

    @Test
    fun testCreateRefreshTokenCreatesIndependentSessions() {
        val firstToken = jwtTokenProvider.createRefreshToken(authentication)
        val secondToken = jwtTokenProvider.createRefreshToken(authentication)
        val savedSessions = argumentCaptor<app.hyuabot.backend.database.entity.RefreshToken>()

        then(refreshTokenRepository).should(times(2)).save(savedSessions.capture())
        assert(firstToken != secondToken)
        assert(
            savedSessions.allValues
                .map { it.uuid }
                .distinct()
                .size == 2,
        )
        assert(savedSessions.allValues.map { it.refreshToken } == listOf(firstToken, secondToken))
    }

    @Test
    fun testGetAuthentication() {
        val token = jwtTokenProvider.createAccessToken(authentication)
        given(userDetailsService.loadUserByUsername("testUser")).willReturn(JWTUser("testUser", ""))
        val auth = jwtTokenProvider.getAuthentication(token)
        assert(auth.isAuthenticated)
        assert((auth.principal as JWTUser).username == "testUser")
    }

    @Test
    fun testGetAuthenticationRejectsTokenFromPreviousPasswordVersion() {
        val token = jwtTokenProvider.createAccessToken(authentication)
        given(userDetailsService.loadUserByUsername("testUser")).willReturn(JWTUser("testUser", "", authVersion = 1))

        assertThrows<BadCredentialsException> { jwtTokenProvider.getAuthentication(token) }
    }

    @Test
    fun testGetAuthenticationRejectsUnexpectedUserDetailsType() {
        val token = jwtTokenProvider.createAccessToken(authentication)
        given(userDetailsService.loadUserByUsername("testUser"))
            .willReturn(SpringUser("testUser", "", emptyList()))

        assertThrows<BadCredentialsException> { jwtTokenProvider.getAuthentication(token) }
    }

    @Test
    fun testGetAuthenticationRejectsInvalidAuthVersionClaim() {
        val key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret))
        val token =
            Jwts
                .builder()
                .subject("testUser")
                .claim("auth_version", "invalid")
                .signWith(key)
                .compact()
        given(userDetailsService.loadUserByUsername("testUser")).willReturn(JWTUser("testUser", ""))

        assertThrows<BadCredentialsException> { jwtTokenProvider.getAuthentication(token) }
    }

    @Test
    fun testInvalidateAccessToken() {
        val accessToken = jwtTokenProvider.createAccessToken("testUser")
        jwtTokenProvider.invalidateAccessToken(accessToken)

        then(valueOperations).should().set(
            startsWith("access_token:"),
            eq("logout"),
            eq(Duration.ofMinutes(expirationMinutes)),
        )
        then(refreshTokenRepository).shouldHaveNoInteractions()
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
