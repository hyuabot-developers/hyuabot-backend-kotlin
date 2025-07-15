package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.RefreshToken
import app.hyuabot.backend.database.entity.User
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.ZonedDateTime
import java.util.UUID

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource("classpath:application-test.properties")
class UserRepositoryTest {
    @Autowired lateinit var userRepository: UserRepository

    @Autowired lateinit var refreshTokenRepository: RefreshTokenRepository

    private val uuid = UUID.randomUUID()
    private val currentTime = ZonedDateTime.now()

    private fun createUser(
        userID: String = "user123",
        password: ByteArray = "password".toByteArray(),
        name: String = "John Doe",
        email: String = "john@example.com",
        phone: String = "1234567890",
        active: Boolean = true,
    ) = User(
        userID = userID,
        password = password,
        name = name,
        email = email,
        phone = phone,
        active = active,
    )

    @BeforeEach
    fun setUp() {
        val user = createUser()
        userRepository.save(user)
        refreshTokenRepository.save(
            RefreshToken(
                uuid = uuid,
                userID = "user123",
                refreshToken = "refreshToken",
                expiredAt = currentTime.plusDays(30),
                createdAt = currentTime,
                updatedAt = currentTime,
                user = user,
            ),
        )
    }

    @AfterEach
    fun tearDown() {
        userRepository.deleteAll()
    }

    @Test
    @DisplayName("User 오브젝트 비교")
    fun testEqualsAndHashCode() {
        val user1 = createUser()
        val user2 = createUser()
        assert(user1 == user2)
        assert(user1.hashCode() == user2.hashCode())
        val user3 = createUser(active = false)
        assert(user1 != user3)
        val user4 = createUser(userID = "user456")
        assert(user1 != user4)
        val user5 = createUser(password = "otherPassword".toByteArray())
        assert(user1 != user5)
        val user6 = createUser(name = "Jane Doe")
        assert(user1 != user6)
        val user7 = createUser(email = "jane@example;com")
        assert(user1 != user7)
        val user8 = createUser(phone = "0987654321")
        assert(user1 != user8)
    }

    @Test
    @DisplayName("UserID로 사용자 조회")
    fun testFindByUserID() {
        val user = userRepository.findByUserID("user123")
        assert(user != null)
        assert(user?.userID == "user123")
        assert(user?.refreshToken?.isEmpty() == true)
        assert(user?.notice?.isEmpty() == true)
        val nonExistentUser = userRepository.findByUserID("nonexistent")
        assert(nonExistentUser == null)
    }

    @Test
    @DisplayName("UserID로 활성화된 사용자 조회")
    fun testFindByUserIDAndActiveIsTrue() {
        val user = userRepository.findByUserIDAndActiveIsTrue("user123")
        assert(user != null)
        assert(user?.userID == "user123")
        val inactiveUser = createUser(userID = "user456", active = false)
        userRepository.save(inactiveUser)
        val inactiveResult = userRepository.findByUserIDAndActiveIsTrue("user456")
        assert(inactiveResult == null)
    }

    @Test
    @DisplayName("이메일로 사용자 조회")
    fun testFindByEmail() {
        val user = userRepository.findByEmail("john@example.com")
        assert(user != null)
        assert(user?.email == "john@example.com")
        val nonExistentUser = userRepository.findByEmail("test@example.com")
        assert(nonExistentUser == null)
    }

    @Test
    @DisplayName("사용자별 인증 토큰 조회")
    fun testFindTokenByUserID() {
        val token = refreshTokenRepository.findByUserID("user123")
        assert(token != null)
        assert(token?.uuid == uuid)
        assert(token?.userID == "user123")
        assert(token?.refreshToken == "refreshToken")
        assert(token?.expiredAt == currentTime.plusDays(30))
        assert(token?.createdAt == currentTime)
        assert(token?.updatedAt == currentTime)
        assert(token?.user?.userID == "user123")
        val nonExistentToken = refreshTokenRepository.findByUserID("nonexistent")
        assert(nonExistentToken == null)
    }

    @Test
    @DisplayName("사용자별 인증 토큰 갱신")
    fun testUpdateTokenByUserID() {
        val token = refreshTokenRepository.findByUserID("user123")
        assert(token != null)
        token?.let {
            it.refreshToken = "newRefreshToken"
            it.expiredAt = currentTime.plusDays(60)
            it.updatedAt = currentTime
            refreshTokenRepository.save(it)
        }
        val updatedToken = refreshTokenRepository.findByUserID("user123")
        assert(updatedToken?.refreshToken == "newRefreshToken")
        assert(updatedToken?.expiredAt == currentTime.plusDays(60))
    }
}
