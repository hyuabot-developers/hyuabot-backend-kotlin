package app.hyuabot.backend.presence

import app.hyuabot.backend.utility.ResponseBuilder
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/presence/shuttle")
class ShuttlePresenceController(
    private val shuttlePresenceService: ShuttlePresenceService,
) {
    @GetMapping
    fun getViewerCounts(): ResponseEntity<ShuttlePresenceCountsResponse> = ResponseEntity.ok(shuttlePresenceService.getViewerCounts())

    @PostMapping
    fun heartbeat(
        @RequestBody request: ShuttlePresenceRequest,
    ): ResponseEntity<*> =
        try {
            ResponseEntity.ok(shuttlePresenceService.heartbeat(request))
        } catch (_: IllegalArgumentException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("INVALID_SHUTTLE_PRESENCE"),
            )
        }
}
