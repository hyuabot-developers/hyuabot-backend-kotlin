package app.hyuabot.backend.auth

import app.hyuabot.backend.auth.domain.CreateUserRequest
import app.hyuabot.backend.database.entity.User
import app.hyuabot.backend.database.repository.UserRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    fun signUp(payload: CreateUserRequest) {
        // 사용자 이름 중복 확인
        userRepository.findByUserID(payload.userID)?.let {
            throw DuplicateKeyException("DUPLICATE_USER_ID")
        }
        // 이메일 중복 확인
        userRepository.findByEmail(payload.email)?.let {
            throw DuplicateKeyException("DUPLICATE_EMAIL")
        }
        // 사용자 생성
        val user =
            User(
                userID = payload.userID,
                password = passwordEncoder.encode(payload.password).toByteArray(),
                name = payload.nickname,
                email = payload.email,
                phone = payload.phone,
                active = false,
            )
        userRepository.save(user)
    }
}
