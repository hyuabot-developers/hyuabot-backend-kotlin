package app.hyuabot.backend.liveactivity.controller

import app.hyuabot.backend.liveactivity.domain.ShuttleLiveActivityRegisterRequest
import app.hyuabot.backend.liveactivity.domain.ShuttleLiveActivityRegisterResponse
import app.hyuabot.backend.liveactivity.service.ShuttleLiveActivityService
import app.hyuabot.backend.utility.ResponseBuilder
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/live-activity/shuttle")
@RestController
class ShuttleLiveActivityController(
    private val service: ShuttleLiveActivityService,
) {
    @PostMapping("")
    fun register(
        @RequestBody request: ShuttleLiveActivityRegisterRequest,
    ): ResponseEntity<ShuttleLiveActivityRegisterResponse> =
        ResponseBuilder.response(
            HttpStatus.CREATED,
            service.register(request),
        )

    @DeleteMapping("/{key}")
    fun unregister(
        @PathVariable key: String,
    ): ResponseEntity<ResponseBuilder.Message> {
        service.unregister(key)
        return ResponseBuilder.response(HttpStatus.OK, "Live Activity registration removed.")
    }
}
