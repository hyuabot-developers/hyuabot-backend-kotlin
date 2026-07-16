package app.hyuabot.backend.adminoverview

import app.hyuabot.backend.security.AdminPermission
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/user/overview")
class AdminOverviewController(
    private val service: AdminOverviewService,
) {
    @GetMapping
    fun getOverview(authentication: Authentication) =
        service.getOverview(
            authentication.authorities
                .mapNotNull { authority ->
                    AdminPermission.entries.find { permission -> permission.name == authority.authority }
                }.toSet(),
        )
}
