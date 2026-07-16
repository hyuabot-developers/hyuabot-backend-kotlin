package app.hyuabot.backend.admin.domain

import app.hyuabot.backend.database.entity.User
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import kotlin.test.assertEquals

class AdminUserResponseTest {
    private val now = ZonedDateTime.parse("2026-07-15T12:00:00+09:00")

    private fun user(
        password: ByteArray? = ByteArray(0),
        active: Boolean = true,
    ) = User(
        userID = "user",
        password = password,
        name = "User",
        email = "user@example.com",
        phone = "",
        active = active,
    )

    @Test
    fun mapsEveryAccountStatus() {
        assertEquals(
            AdminUserStatus.DELETED,
            AdminUserResponse.from(user().apply { deletedAt = now }, now = now).status,
        )
        assertEquals(
            AdminUserStatus.PENDING_SETUP,
            AdminUserResponse.from(user(password = null), now.plusMinutes(1), now).status,
        )
        assertEquals(
            AdminUserStatus.INVITATION_EXPIRED,
            AdminUserResponse.from(user(password = null), now.minusMinutes(1), now).status,
        )
        assertEquals(
            AdminUserStatus.INVITATION_EXPIRED,
            AdminUserResponse.from(user(password = null), null, now).status,
        )
        assertEquals(AdminUserStatus.ACTIVE, AdminUserResponse.from(user(), now = now).status)
        assertEquals(AdminUserStatus.INACTIVE, AdminUserResponse.from(user(active = false), now = now).status)
    }
}
