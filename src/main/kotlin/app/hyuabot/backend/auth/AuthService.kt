package app.hyuabot.backend.auth

import app.hyuabot.backend.auth.domain.ChangePasswordRequest
import app.hyuabot.backend.auth.domain.TokenResponse
import app.hyuabot.backend.auth.domain.UpdateProfileRequest
import app.hyuabot.backend.auth.exception.DuplicateEmailException
import app.hyuabot.backend.auth.exception.InvalidUserInputException
import app.hyuabot.backend.database.entity.User
import app.hyuabot.backend.database.repository.RefreshTokenRepository
import app.hyuabot.backend.database.repository.UserRepository
import app.hyuabot.backend.security.JWTTokenProvider
import app.hyuabot.backend.security.JWTUser
import jakarta.servlet.http.HttpServletRequest
import jakarta.transaction.Transactional
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManagerBuilder: AuthenticationManagerBuilder,
    private val tokenProvider: JWTTokenProvider,
) {
    @Transactional
    fun login(
        userID: String,
        password: String,
    ): TokenResponse {
        val authenticationToken = UsernamePasswordAuthenticationToken(userID, password)
        val authentication = authenticationManagerBuilder.`object`.authenticate(authenticationToken)
        val accessToken = tokenProvider.createAccessToken(authentication)
        val refreshToken = tokenProvider.createRefreshToken(authentication)
        return TokenResponse(accessToken = accessToken, refreshToken = refreshToken)
    }

    fun refreshToken(refreshToken: String): String {
        val authentication = tokenProvider.getAuthentication(refreshToken)
        val userID = (authentication.principal as JWTUser).username
        if (refreshTokenRepository.findByUserID(userID)?.refreshToken != refreshToken) {
            throw BadCredentialsException("INVALID_REFRESH_TOKEN")
        }
        return tokenProvider.createAccessToken(authentication)
    }

    fun getUserInfo(userID: String): User =
        userRepository.findByUserIDAndActiveIsTrue(userID)
            ?: throw IllegalArgumentException("NO_USER_INFO")

    @Transactional
    fun updateProfile(
        userID: String,
        request: UpdateProfileRequest,
    ): User {
        val user = getUserInfo(userID)
        val nickname = request.nickname.trim()
        val email = request.email.trim().lowercase()
        val phone = request.phone.trim()
        if (
            nickname.isEmpty() ||
            nickname.length > 20 ||
            email.isEmpty() ||
            email.length > 50 ||
            phone.length > 15
        ) {
            throw InvalidUserInputException("INVALID_USER_INPUT")
        }
        userRepository.findByEmailIgnoreCase(email)?.let {
            if (it.userID != userID) throw DuplicateEmailException()
        }
        user.name = nickname
        user.email = email
        user.phone = phone
        return userRepository.save(user)
    }

    @Transactional
    fun changePassword(
        userID: String,
        request: ChangePasswordRequest,
    ) {
        UserInvitationService.validatePassword(request.newPassword)
        val user = getUserInfo(userID)
        val encodedPassword = user.password?.decodeToString()
        if (encodedPassword == null || !passwordEncoder.matches(request.currentPassword, encodedPassword)) {
            throw BadCredentialsException("CURRENT_PASSWORD_MISMATCH")
        }
        user.password =
            (passwordEncoder.encode(request.newPassword) ?: throw InvalidUserInputException("INVALID_PASSWORD"))
                .toByteArray()
        user.authVersion += 1
        userRepository.save(user)
        refreshTokenRepository.findByUserID(userID)?.let(refreshTokenRepository::delete)
    }

    fun logout(
        userInfo: User,
        request: HttpServletRequest,
    ) {
        // Access token 무효화
        tokenProvider.invalidateAccessToken(
            user = userInfo,
            accessToken =
                request.cookies.firstOrNull { it.name == "access_token" }?.value
                    ?: throw IllegalArgumentException("NO_ACCESS_TOKEN"),
        )
        refreshTokenRepository.findByUserID(userInfo.userID)?.let { refreshToken ->
            refreshTokenRepository.delete(refreshToken)
        } ?: throw IllegalArgumentException("NO_REFRESH_TOKEN")
    }
}
