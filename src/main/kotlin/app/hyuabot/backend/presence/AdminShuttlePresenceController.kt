package app.hyuabot.backend.presence

import app.hyuabot.backend.utility.ResponseBuilder
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/shuttle-presence")
class AdminShuttlePresenceController(
    private val adminShuttlePresenceService: AdminShuttlePresenceService,
) {
    @GetMapping
    fun getViewerCounts(): ResponseEntity<AdminShuttlePresenceResponse> =
        ResponseBuilder.response(
            HttpStatus.OK,
            adminShuttlePresenceService.getViewerCounts(),
        )
}
