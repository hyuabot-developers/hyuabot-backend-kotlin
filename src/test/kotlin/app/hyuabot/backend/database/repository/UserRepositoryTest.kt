package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.RefreshToken
import app.hyuabot.backend.database.entity.User
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
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
        // User 엔티티의 동일성은 자연키(userID) 기준이다.
        // 같은 userID는 같은 엔티티로 간주되며, 가변 필드 값과 무관하다.
        val user1 = createUser()
        val user2 = createUser()
        assert(user1 == user2)
        assert(user1.hashCode() == user2.hashCode())

        // 같은 userID라면 다른 필드(active/password/name/email/phone)가 달라도 동일 엔티티
        assert(user1 == createUser(active = false))
        assert(user1 == createUser(password = "otherPassword".toByteArray()))
        assert(user1 == createUser(name = "Jane Doe"))
        assert(user1 == createUser(email = "jane@example.com"))
        assert(user1 == createUser(phone = "0987654321"))

        // userID가 다르면 다른 엔티티
        val other = createUser(userID = "user456")
        assert(user1 != other)
        assert(user1.hashCode() != other.hashCode())
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
        val inactiveUser = createUser(userID = "user456", email = "user456@example.com", active = false)
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
    @DisplayName("사용자와 토큰 값으로 인증 세션 조회")
    fun testFindTokenByUserIDAndRefreshToken() {
        val token = refreshTokenRepository.findByUserIDAndRefreshToken("user123", "refreshToken")
        assert(token != null)
        assert(token?.uuid == uuid)
        assert(token?.userID == "user123")
        assert(token?.refreshToken == "refreshToken")
        assert(token?.expiredAt == currentTime.plusDays(30))
        assert(token?.createdAt == currentTime)
        assert(token?.updatedAt == currentTime)
        assert(token?.user?.userID == "user123")
        val nonExistentToken =
            refreshTokenRepository.findByUserIDAndRefreshToken("user123", "nonexistentRefreshToken")
        assert(nonExistentToken == null)
    }

    @Test
    @DisplayName("한 사용자의 여러 인증 세션 저장 및 전체 삭제")
    fun testMultipleSessionsAndDeleteAllByUserID() {
        refreshTokenRepository.save(
            RefreshToken(
                uuid = UUID.randomUUID(),
                userID = "user123",
                refreshToken = "secondRefreshToken",
                expiredAt = currentTime.plusDays(30),
                createdAt = currentTime,
                updatedAt = currentTime,
                user = userRepository.findByUserID("user123"),
            ),
        )

        assert(refreshTokenRepository.findByUserIDAndRefreshToken("user123", "refreshToken") != null)
        assert(refreshTokenRepository.findByUserIDAndRefreshToken("user123", "secondRefreshToken") != null)

        refreshTokenRepository.deleteAllByUserID("user123")

        assert(refreshTokenRepository.findByUserIDAndRefreshToken("user123", "refreshToken") == null)
        assert(refreshTokenRepository.findByUserIDAndRefreshToken("user123", "secondRefreshToken") == null)
    }
}
