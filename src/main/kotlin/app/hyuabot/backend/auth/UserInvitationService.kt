package app.hyuabot.backend.auth

import app.hyuabot.backend.admin.exception.AdminUserNotFoundException
import app.hyuabot.backend.auth.domain.InvitationValidationResponse
import app.hyuabot.backend.auth.exception.InvalidInvitationException
import app.hyuabot.backend.auth.exception.InvalidUserInputException
import app.hyuabot.backend.database.entity.AdminUserInvitation
import app.hyuabot.backend.database.repository.AdminUserInvitationRepository
import app.hyuabot.backend.database.repository.UserRepository
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import jakarta.transaction.Transactional
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.ZonedDateTime
import java.util.Base64
import java.util.UUID

data class IssuedInvitation(
    val token: String,
    val expiresAt: ZonedDateTime,
)

@Service
class UserInvitationService(
    private val invitationRepository: AdminUserInvitationRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    private val secureRandom = SecureRandom()

    @Transactional
    fun issue(
        userID: String,
        createdBy: String,
    ): IssuedInvitation {
        val user = userRepository.findByUserID(userID) ?: throw AdminUserNotFoundException()
        if (user.password != null) {
            throw InvalidInvitationException()
        }

        val now = now()
        val activeInvitations = invitationRepository.findAllByUserIDAndConsumedAtIsNullAndRevokedAtIsNull(userID)
        activeInvitations.forEach { it.revokedAt = now }
        invitationRepository.saveAllAndFlush(activeInvitations)

        val token = generateToken()
        val expiresAt = now.plusHours(INVITATION_VALID_HOURS)
        invitationRepository.save(
            AdminUserInvitation(
                uuid = UUID.randomUUID(),
                userID = userID,
                tokenHash = hash(token),
                createdBy = createdBy,
                expiresAt = expiresAt,
                createdAt = now,
            ),
        )
        return IssuedInvitation(token, expiresAt)
    }

    fun validate(token: String): InvitationValidationResponse {
        val invitation =
            invitationRepository.findByTokenHashAndConsumedAtIsNullAndRevokedAtIsNull(hash(token))
                ?: return InvitationValidationResponse(false, null)
        val valid = invitation.expiresAt.isAfter(now())
        return InvitationValidationResponse(valid, invitation.expiresAt)
    }

    @Transactional
    fun complete(
        token: String,
        password: String,
    ) {
        validatePassword(password)
        val invitation =
            invitationRepository.findActiveForUpdate(hash(token))
                ?: throw InvalidInvitationException()
        val now = now()
        if (!invitation.expiresAt.isAfter(now)) {
            throw InvalidInvitationException()
        }
        val user = userRepository.findByUserID(invitation.userID) ?: throw InvalidInvitationException()
        if (user.password != null) {
            throw InvalidInvitationException()
        }

        user.password =
            (passwordEncoder.encode(password) ?: throw InvalidUserInputException("INVALID_PASSWORD"))
                .toByteArray()
        user.active = true
        userRepository.save(user)
        invitation.consumedAt = now
        invitationRepository.save(invitation)
    }

    internal fun hash(token: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(token.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun generateToken(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun now(): ZonedDateTime = ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone)

    companion object {
        const val INVITATION_VALID_HOURS = 24L
        const val TOKEN_BYTES = 32

        fun validatePassword(password: String) {
            val byteLength = password.toByteArray().size
            if (password.length < 15 || byteLength > 72) {
                throw InvalidUserInputException("INVALID_PASSWORD")
            }
        }
    }
}
