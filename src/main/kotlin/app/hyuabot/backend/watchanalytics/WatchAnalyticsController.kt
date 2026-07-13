package app.hyuabot.backend.watchanalytics

import app.hyuabot.backend.utility.ResponseBuilder
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/analytics/watch")
class WatchAnalyticsController(
    private val watchAnalyticsService: WatchAnalyticsService,
) {
    @PostMapping("/events")
    fun recordEvent(
        @RequestBody request: WatchAnalyticsEventRequest,
    ): ResponseEntity<*> =
        try {
            watchAnalyticsService.record(request)
            ResponseEntity.accepted().build<Void>()
        } catch (_: IllegalArgumentException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("INVALID_WATCH_ANALYTICS_EVENT"),
            )
        }
}
