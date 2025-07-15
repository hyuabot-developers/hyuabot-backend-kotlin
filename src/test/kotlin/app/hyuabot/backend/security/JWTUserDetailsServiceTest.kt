package app.hyuabot.backend.security

import app.hyuabot.backend.database.entity.User
import app.hyuabot.backend.database.repository.UserRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.security.core.userdetails.UsernameNotFoundException

@ExtendWith(MockitoExtension::class)
class JWTUserDetailsServiceTest {
    @Mock
    lateinit var userRepository: UserRepository

    @InjectMocks
    lateinit var jwtUserDetailsService: JWTUserDetailsService

    @Test
    @DisplayName("사용자 ID로 사용자 조회 테스트")
    fun loadUserByUsernameTest() {
        val userID = "testUser"
        val user =
            User(
                userID = userID,
                password = "testPassword".toByteArray(),
                name = "Test User",
                email = "test@example.com",
                phone = "1234567890",
                active = true,
            )
        whenever(userRepository.findByUserIDAndActiveIsTrue(userID)).thenReturn(user)
        val userDetails = jwtUserDetailsService.loadUserByUsername(userID)
        assert(userDetails.username == userID)
        assert(userDetails.password == "testPassword")
        assert(userDetails.isEnabled)
        assert(userDetails.isAccountNonExpired)
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 조회 시 예외 발생 테스트")
    fun loadUserByUsernameNotFoundTest() {
        val userID = "nonExistentUser"
        whenever(userRepository.findByUserIDAndActiveIsTrue(userID)).thenReturn(null)
        assertThrows<UsernameNotFoundException> { jwtUserDetailsService.loadUserByUsername(userID) }
    }
}
