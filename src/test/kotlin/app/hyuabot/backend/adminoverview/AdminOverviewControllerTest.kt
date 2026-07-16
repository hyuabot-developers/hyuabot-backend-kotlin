package app.hyuabot.backend.adminoverview

import app.hyuabot.backend.adminoverview.domain.AdminOverviewResponse
import app.hyuabot.backend.security.AdminPermission
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority

class AdminOverviewControllerTest {
    @Test
    fun `controller passes recognized authorities to the service`() {
        val service = mock<AdminOverviewService>()
        val authentication = mock<Authentication>()
        val response = AdminOverviewResponse("now", emptyList(), null, "grafana")
        whenever(authentication.authorities).thenReturn(
            listOf(SimpleGrantedAuthority("BUS"), SimpleGrantedAuthority("UNKNOWN")),
        )
        whenever(service.getOverview(setOf(AdminPermission.BUS))).thenReturn(response)

        assertEquals(response, AdminOverviewController(service).getOverview(authentication))
        val captor = argumentCaptor<Set<AdminPermission>>()
        verify(service).getOverview(captor.capture())
        assertEquals(setOf(AdminPermission.BUS), captor.firstValue)
    }
}
