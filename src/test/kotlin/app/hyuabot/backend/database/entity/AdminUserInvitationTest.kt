package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdminUserInvitationTest {
    private val uuidA = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val uuidB = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val now = ZonedDateTime.parse("2026-07-15T12:00:00+09:00")

    private fun create(uuid: UUID = uuidA) =
        AdminUserInvitation(
            uuid = uuid,
            userID = "new-user",
            tokenHash = "a".repeat(64),
            createdBy = "jil8885",
            expiresAt = now.plusHours(24),
            consumedAt = null,
            revokedAt = null,
            createdAt = now,
        )

    @Test
    fun equalsAndHashCode() {
        val invitation = create()
        val nullValue: Any? = null

        assertTrue(invitation == invitation)
        assertFalse(invitation.equals(nullValue))
        assertFalse(invitation.equals(Any()))
        assertEquals(invitation, create())
        assertEquals(invitation.hashCode(), create().hashCode())
        assertFalse(invitation == create(uuid = uuidB))
    }

    @Test
    fun propertiesAndMutators() {
        val invitation = create()

        assertEquals(uuidA, invitation.uuid)
        assertEquals("new-user", invitation.userID)
        assertEquals("a".repeat(64), invitation.tokenHash)
        assertEquals("jil8885", invitation.createdBy)
        assertEquals(now.plusHours(24), invitation.expiresAt)
        assertEquals(now, invitation.createdAt)
        invitation.consumedAt = now.plusMinutes(1)
        invitation.revokedAt = now.plusMinutes(2)
        assertEquals(now.plusMinutes(1), invitation.consumedAt)
        assertEquals(now.plusMinutes(2), invitation.revokedAt)
    }

    @Test
    fun noArgConstructor() {
        val invitation = AdminUserInvitation::class.java.getDeclaredConstructor().newInstance()

        assertNotNull(invitation)
    }
}
