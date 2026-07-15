package app.hyuabot.backend.security

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminPermissionTest {
    @Test
    fun superAdminExpandsToEveryPermission() {
        assertEquals(AdminPermission.entries.toSet(), setOf(AdminPermission.SUPER_ADMIN).effectivePermissions())
    }

    @Test
    fun managementPermissionRemainsScoped() {
        val permissions = setOf(AdminPermission.SHUTTLE)
        assertEquals(permissions, permissions.effectivePermissions())
        assertTrue(AdminPermission.SUPER_ADMIN !in AdminPermission.managementPermissions)
    }
}
