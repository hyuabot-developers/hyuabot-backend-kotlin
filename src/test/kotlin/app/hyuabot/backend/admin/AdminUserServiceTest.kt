package app.hyuabot.backend.admin

import app.hyuabot.backend.admin.domain.AdminUserStatus
import app.hyuabot.backend.admin.domain.CreateAdminUserRequest
import app.hyuabot.backend.admin.domain.UpdateAdminUserRequest
import app.hyuabot.backend.admin.exception.AdminUserNotFoundException
import app.hyuabot.backend.admin.exception.LastSuperAdminException
import app.hyuabot.backend.admin.exception.PendingUserActivationException
import app.hyuabot.backend.auth.IssuedInvitation
import app.hyuabot.backend.auth.UserInvitationService
import app.hyuabot.backend.auth.exception.DuplicateEmailException
import app.hyuabot.backend.auth.exception.DuplicateUserIDException
import app.hyuabot.backend.auth.exception.InvalidUserInputException
import app.hyuabot.backend.database.entity.AdminUserInvitation
import app.hyuabot.backend.database.entity.User
import app.hyuabot.backend.database.repository.AdminUserInvitationRepository
import app.hyuabot.backend.database.repository.UserRepository
import app.hyuabot.backend.security.AdminPermission
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class AdminUserServiceTest {
    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var invitationRepository: AdminUserInvitationRepository

    @Mock
    lateinit var invitationService: UserInvitationService

    @InjectMocks
    lateinit var service: AdminUserService

    private fun user(
        id: String = "user",
        active: Boolean = true,
        permissions: Set<AdminPermission> = setOf(AdminPermission.SHUTTLE),
    ) = User(
        userID = id,
        password = ByteArray(0),
        name = "User",
        email = "user@example.com",
        phone = "01000000000",
        active = active,
        permissions = permissions.toMutableSet(),
    )

    @Test
    fun getUsersMapsRepositoryResult() {
        whenever(userRepository.findAllByOrderByNameAscUserIDAsc()).thenReturn(listOf(user()))
        whenever(invitationRepository.findAllByConsumedAtIsNullAndRevokedAtIsNull()).thenReturn(emptyList())

        val result = service.getUsers()

        assertEquals(listOf(AdminPermission.SHUTTLE), result.single().permissions)
    }

    @Test
    fun getUsersMapsPendingInvitationStatus() {
        val user = user().apply { password = null }
        val expiry = ZonedDateTime.now().plusHours(1)
        val invitation =
            AdminUserInvitation(
                UUID.randomUUID(),
                user.userID,
                "hash",
                "creator",
                expiry,
                createdAt = ZonedDateTime.now(),
            )
        whenever(userRepository.findAllByOrderByNameAscUserIDAsc()).thenReturn(listOf(user))
        whenever(invitationRepository.findAllByConsumedAtIsNullAndRevokedAtIsNull()).thenReturn(listOf(invitation))

        val result = service.getUsers().single()

        assertEquals(AdminUserStatus.PENDING_SETUP, result.status)
        assertEquals(expiry, result.invitationExpiresAt)
    }

    @Test
    fun createUserSavesPendingUserAndReturnsOneTimeInvitation() {
        val request =
            CreateAdminUserRequest(
                " new-user ",
                " New User ",
                " NEW@EXAMPLE.COM ",
                " 01012345678 ",
                setOf(AdminPermission.BUS),
            )
        val expiry = ZonedDateTime.now().plusHours(24)
        whenever(userRepository.saveAndFlush(org.mockito.kotlin.any<User>())).thenAnswer { it.arguments[0] as User }
        whenever(invitationService.issue("new-user", "creator")).thenReturn(IssuedInvitation("token", expiry))

        val result = service.createUser(request, "creator")

        assertEquals("new-user", result.user.username)
        assertEquals("new@example.com", result.user.email)
        assertEquals(AdminUserStatus.PENDING_SETUP, result.user.status)
        assertEquals("token", result.token)
        verify(invitationService).issue("new-user", "creator")
    }

    @Test
    fun createUserRejectsDuplicateAndInvalidInput() {
        val valid = CreateAdminUserRequest("user", "User", "user@example.com", "", emptySet())
        whenever(userRepository.findByUserID("user")).thenReturn(user())
        assertThrows<DuplicateUserIDException> { service.createUser(valid, "creator") }

        whenever(userRepository.findByUserID("user")).thenReturn(null)
        whenever(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(user())
        assertThrows<DuplicateEmailException> { service.createUser(valid, "creator") }

        val invalid = valid.copy(nickname = " ")
        assertThrows<InvalidUserInputException> { service.createUser(invalid, "creator") }
        assertThrows<InvalidUserInputException> { service.createUser(valid.copy(userID = " "), "creator") }
        assertThrows<InvalidUserInputException> { service.createUser(valid.copy(email = " "), "creator") }
    }

    @Test
    fun reissueInvitationReturnsTokenAndRejectsMissingUser() {
        val user = user().apply { password = null }
        val expiry = ZonedDateTime.now().plusHours(24)
        whenever(userRepository.findByUserID("user")).thenReturn(user)
        whenever(invitationService.issue("user", "creator")).thenReturn(IssuedInvitation("token", expiry))

        val result = service.reissueInvitation("user", "creator")

        assertEquals("token", result.token)
        assertEquals(AdminUserStatus.PENDING_SETUP, result.user.status)

        whenever(userRepository.findByUserID("missing")).thenReturn(null)
        assertThrows<AdminUserNotFoundException> { service.reissueInvitation("missing", "creator") }
    }

    @Test
    fun updateUserChangesActivationAndPermissions() {
        val user = user()
        whenever(userRepository.findAllForPermissionUpdate()).thenReturn(listOf(user))
        whenever(userRepository.save(user)).thenReturn(user)

        val result =
            service.updateUser(
                "user",
                UpdateAdminUserRequest(false, setOf(AdminPermission.BUS)),
            )

        assertFalse(result.active)
        assertEquals(listOf(AdminPermission.BUS), result.permissions)
        verify(userRepository).save(user)
    }

    @Test
    fun updateUserRejectsMissingUser() {
        whenever(userRepository.findAllForPermissionUpdate()).thenReturn(emptyList())

        assertThrows<AdminUserNotFoundException> {
            service.updateUser("missing", UpdateAdminUserRequest(true, emptySet()))
        }
    }

    @Test
    fun updateUserProtectsLastActiveSuperAdmin() {
        val user = user(permissions = setOf(AdminPermission.SUPER_ADMIN))
        whenever(userRepository.findAllForPermissionUpdate()).thenReturn(listOf(user))

        assertThrows<LastSuperAdminException> {
            service.updateUser("user", UpdateAdminUserRequest(true, setOf(AdminPermission.SHUTTLE)))
        }
    }

    @Test
    fun updateUserAllowsChangingSuperAdminWhenAnotherRemains() {
        val user = user(permissions = setOf(AdminPermission.SUPER_ADMIN))
        val otherSuperAdmin = user(id = "other", permissions = setOf(AdminPermission.SUPER_ADMIN))
        whenever(userRepository.findAllForPermissionUpdate()).thenReturn(listOf(user, otherSuperAdmin))
        whenever(userRepository.save(user)).thenReturn(user)

        val result = service.updateUser("user", UpdateAdminUserRequest(true, setOf(AdminPermission.NOTICE)))

        assertEquals(listOf(AdminPermission.NOTICE), result.permissions)
    }

    @Test
    fun updateUserAllowsDeactivatingSuperAdminWhenAnotherActiveSuperAdminRemains() {
        val user = user(permissions = setOf(AdminPermission.SUPER_ADMIN))
        val inactiveSuperAdmin = user(id = "inactive", active = false, permissions = setOf(AdminPermission.SUPER_ADMIN))
        val regularAdmin = user(id = "regular", permissions = setOf(AdminPermission.BUS))
        val otherSuperAdmin = user(id = "other", permissions = setOf(AdminPermission.SUPER_ADMIN))
        whenever(userRepository.findAllForPermissionUpdate())
            .thenReturn(listOf(user, inactiveSuperAdmin, regularAdmin, otherSuperAdmin))
        whenever(userRepository.save(user)).thenReturn(user)

        val result = service.updateUser("user", UpdateAdminUserRequest(false, setOf(AdminPermission.BUS)))

        assertFalse(result.active)
        assertEquals(listOf(AdminPermission.BUS), result.permissions)
    }

    @Test
    fun updateUserAllowsRetainingLastActiveSuperAdmin() {
        val user = user(permissions = setOf(AdminPermission.SUPER_ADMIN))
        whenever(userRepository.findAllForPermissionUpdate()).thenReturn(listOf(user))
        whenever(userRepository.save(user)).thenReturn(user)

        val result =
            service.updateUser(
                "user",
                UpdateAdminUserRequest(true, setOf(AdminPermission.SUPER_ADMIN, AdminPermission.NOTICE)),
            )

        assertTrue(result.active)
        assertEquals(listOf(AdminPermission.SUPER_ADMIN, AdminPermission.NOTICE), result.permissions)
    }

    @Test
    fun updateUserAllowsUpdatingInactiveSuperAdmin() {
        val user = user(active = false, permissions = setOf(AdminPermission.SUPER_ADMIN))
        whenever(userRepository.findAllForPermissionUpdate()).thenReturn(listOf(user))
        whenever(userRepository.save(user)).thenReturn(user)

        val result = service.updateUser("user", UpdateAdminUserRequest(true, setOf(AdminPermission.SHUTTLE)))

        assertTrue(result.active)
        assertEquals(listOf(AdminPermission.SHUTTLE), result.permissions)
    }

    @Test
    fun updateUserCannotActivatePendingUser() {
        val user = user(active = false).apply { password = null }
        whenever(userRepository.findAllForPermissionUpdate()).thenReturn(listOf(user))

        assertThrows<PendingUserActivationException> {
            service.updateUser("user", UpdateAdminUserRequest(true, emptySet()))
        }
    }
}
