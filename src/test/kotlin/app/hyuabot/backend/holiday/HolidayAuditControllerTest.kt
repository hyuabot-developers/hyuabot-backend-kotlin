package app.hyuabot.backend.holiday

import app.hyuabot.backend.holiday.audit.HolidayAuditController
import app.hyuabot.backend.holiday.audit.HolidayAuditResult
import app.hyuabot.backend.holiday.audit.HolidayAuditService
import app.hyuabot.backend.security.AdminPermission
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.ZoneId
import java.time.ZonedDateTime

class HolidayAuditControllerTest {
    @Test
    fun `controller maps known authorities to audit permissions`() {
        val service = mock<HolidayAuditService>()
        val now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
        val expected = HolidayAuditResult(now, null, emptyList())
        val permissions = argumentCaptor<Set<AdminPermission>>()
        whenever(service.audit(permissions.capture(), any())).thenReturn(expected)
        val authentication =
            UsernamePasswordAuthenticationToken(
                "admin",
                null,
                listOf(SimpleGrantedAuthority("BUS"), SimpleGrantedAuthority("UNKNOWN")),
            )

        val result = HolidayAuditController(service).getAudit(authentication)

        assertEquals(expected, result)
        assertEquals(setOf(AdminPermission.BUS), permissions.firstValue)
    }
}
