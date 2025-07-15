package app.hyuabot.backend.security

import app.hyuabot.backend.database.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class JWTUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user =
            userRepository.findByUserIDAndActiveIsTrue(username)
                ?: throw UsernameNotFoundException("NO_USER_INFO")

        return JWTUser(
            username = user.userID,
            password = user.password.decodeToString(),
        )
    }
}
