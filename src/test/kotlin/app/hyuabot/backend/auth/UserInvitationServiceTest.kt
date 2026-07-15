package app.hyuabot.backend.auth

import app.hyuabot.backend.admin.exception.AdminUserNotFoundException
import app.hyuabot.backend.auth.exception.InvalidInvitationException
import app.hyuabot.backend.auth.exception.InvalidUserInputException
import app.hyuabot.backend.database.entity.AdminUserInvitation
import app.hyuabot.backend.database.entity.User
import app.hyuabot.backend.database.repository.AdminUserInvitationRepository
import app.hyuabot.backend.database.repository.UserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class UserInvitationServiceTest {
    @Mock lateinit var invitationRepository: AdminUserInvitationRepository

    @Mock lateinit var userRepository: UserRepository

    @Mock lateinit var passwordEncoder: PasswordEncoder

    @Captor lateinit var invitationCaptor: ArgumentCaptor<AdminUserInvitation>

    private val service by lazy { UserInvitationService(invitationRepository, userRepository, passwordEncoder) }

    private fun user(password: ByteArray? = null) = User("new-user", password, "New User", "new@example.com", "", false)

    private fun invitation(
        expiresAt: ZonedDateTime = ZonedDateTime.now().plusHours(1),
        tokenHash: String = service.hash("token"),
    ) = AdminUserInvitation(
        UUID.randomUUID(),
        "new-user",
        tokenHash,
        "jil8885",
        expiresAt,
        createdAt = ZonedDateTime.now().minusMinutes(1),
    )

    @Test
    fun issueRevokesPreviousInvitationAndStoresOnlyTokenHash() {
        val user = user()
        val previous = invitation()
        whenever(userRepository.findByUserID("new-user")).thenReturn(user)
        whenever(invitationRepository.findAllByUserIDAndConsumedAtIsNullAndRevokedAtIsNull("new-user"))
            .thenReturn(listOf(previous))

        val result = service.issue("new-user", "jil8885")

        assertNotNull(previous.revokedAt)
        assertEquals(43, result.token.length)
        verify(invitationRepository).saveAllAndFlush(listOf(previous))
        verify(invitationRepository).save(invitationCaptor.capture())
        val saved = invitationCaptor.value
        assertEquals(service.hash(result.token), saved.tokenHash)
        assertNotEquals(result.token, saved.tokenHash)
        assertEquals("jil8885", saved.createdBy)
        assertTrue(saved.expiresAt.isAfter(saved.createdAt.plusHours(23)))
    }

    @Test
    fun issueRejectsMissingOrAlreadyConfiguredUser() {
        whenever(userRepository.findByUserID("missing")).thenReturn(null)
        assertThrows<AdminUserNotFoundException> { service.issue("missing", "jil8885") }

        whenever(userRepository.findByUserID("new-user")).thenReturn(user("encoded".toByteArray()))
        assertThrows<InvalidInvitationException> { service.issue("new-user", "jil8885") }
    }

    @Test
    fun validateReturnsInvitationStateWithoutExposingUnknownExpiry() {
        whenever(invitationRepository.findByTokenHashAndConsumedAtIsNullAndRevokedAtIsNull(service.hash("missing")))
            .thenReturn(null)
        assertEquals(null, service.validate("missing").expiresAt)
        assertFalse(service.validate("missing").valid)

        val valid = invitation()
        whenever(invitationRepository.findByTokenHashAndConsumedAtIsNullAndRevokedAtIsNull(service.hash("token")))
            .thenReturn(valid)
        assertTrue(service.validate("token").valid)
        assertEquals(valid.expiresAt, service.validate("token").expiresAt)

        val expired = invitation(expiresAt = ZonedDateTime.now().minusMinutes(1))
        whenever(invitationRepository.findByTokenHashAndConsumedAtIsNullAndRevokedAtIsNull(service.hash("expired")))
            .thenReturn(expired)
        assertFalse(service.validate("expired").valid)
    }

    @Test
    fun completeActivatesUserConsumesInvitationAndHashesPassword() {
        val invitation = invitation()
        val user = user()
        whenever(invitationRepository.findActiveForUpdate(service.hash("token"))).thenReturn(invitation)
        whenever(userRepository.findByUserID("new-user")).thenReturn(user)
        whenever(passwordEncoder.encode("a-secure-password")).thenReturn("encoded")

        service.complete("token", "a-secure-password")

        assertTrue(user.active)
        assertEquals("encoded", user.password?.decodeToString())
        assertNotNull(invitation.consumedAt)
        verify(userRepository).save(user)
        verify(invitationRepository).save(invitation)
    }

    @Test
    fun completeRejectsInvalidInvitationStates() {
        assertThrows<InvalidInvitationException> { service.complete("missing", "a-secure-password") }

        val expired = invitation(expiresAt = ZonedDateTime.now().minusMinutes(1))
        whenever(invitationRepository.findActiveForUpdate(service.hash("expired"))).thenReturn(expired)
        assertThrows<InvalidInvitationException> { service.complete("expired", "a-secure-password") }

        val valid = invitation()
        whenever(invitationRepository.findActiveForUpdate(service.hash("unknown-user"))).thenReturn(valid)
        whenever(userRepository.findByUserID("new-user")).thenReturn(null)
        assertThrows<InvalidInvitationException> { service.complete("unknown-user", "a-secure-password") }

        whenever(invitationRepository.findActiveForUpdate(service.hash("configured"))).thenReturn(valid)
        whenever(userRepository.findByUserID("new-user")).thenReturn(user("encoded".toByteArray()))
        assertThrows<InvalidInvitationException> { service.complete("configured", "a-secure-password") }
    }

    @Test
    fun completeRejectsInvalidPasswordAndEncoderFailure() {
        assertThrows<InvalidUserInputException> { service.complete("token", "too-short") }
        assertThrows<InvalidUserInputException> { service.complete("token", "가".repeat(25)) }

        val invitation = invitation()
        whenever(invitationRepository.findActiveForUpdate(service.hash("token"))).thenReturn(invitation)
        whenever(userRepository.findByUserID("new-user")).thenReturn(user())
        whenever(passwordEncoder.encode(any())).thenReturn(null)
        assertThrows<InvalidUserInputException> { service.complete("token", "a-secure-password") }
        assertNull(invitation.consumedAt)
    }

    @Test
    fun hashIsStableAndUsesSha256Hex() {
        assertEquals(service.hash("same"), service.hash("same"))
        assertEquals(64, service.hash("same").length)
        assertNotEquals(service.hash("same"), service.hash("different"))
    }
}
