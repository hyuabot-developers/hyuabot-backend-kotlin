package app.hyuabot.backend.holiday.audit

import app.hyuabot.backend.security.AdminPermission
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/user/overview/holiday-audit")
class HolidayAuditController(
    private val service: HolidayAuditService,
) {
    @GetMapping
    fun getAudit(authentication: Authentication): HolidayAuditResult =
        service.audit(
            authentication.authorities
                .mapNotNull { authority -> AdminPermission.entries.find { it.name == authority.authority } }
                .toSet(),
        )
}
