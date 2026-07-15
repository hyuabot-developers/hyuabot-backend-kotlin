package app.hyuabot.backend.admin

import app.hyuabot.backend.admin.domain.UpdateAdminUserRequest
import app.hyuabot.backend.admin.exception.AdminUserNotFoundException
import app.hyuabot.backend.admin.exception.LastSuperAdminException
import app.hyuabot.backend.database.entity.User
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class AdminUserServiceTest {
    @Mock
    lateinit var userRepository: UserRepository

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

        val result = service.getUsers()

        assertEquals(listOf(AdminPermission.SHUTTLE), result.single().permissions)
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
}
