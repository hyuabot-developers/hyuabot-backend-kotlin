package app.hyuabot.backend.shuttle

import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/shuttle")
@RestController
@Tag(name = "Shuttle", description = "셔틀버스 관련 API")
class ShuttleController {
    private val logger = LoggerFactory.getLogger(javaClass)
}
