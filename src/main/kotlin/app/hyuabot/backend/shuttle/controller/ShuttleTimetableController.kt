package app.hyuabot.backend.shuttle.controller

import app.hyuabot.backend.shuttle.domain.ShuttleTimetableViewListResponse
import app.hyuabot.backend.shuttle.domain.ShuttleTimetableViewResponse
import app.hyuabot.backend.shuttle.service.ShuttleTimetableService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.format.DateTimeFormatter

@RequestMapping("/api/v1/shuttle/timetable")
@RestController
@Tag(name = "Shuttle", description = "셔틀버스 관련 API")
class ShuttleTimetableController {
    @Autowired private lateinit var service: ShuttleTimetableService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("")
    @Operation(summary = "셔틀버스 시간표 조회", description = "셔틀버스 시간표를 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "셔틀버스 시간표 조회 성공",
        content = [Content(schema = Schema(implementation = ShuttleTimetableViewListResponse::class))],
    )
    fun getShuttleTimetable(): ShuttleTimetableViewListResponse {
        val departureTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        return ShuttleTimetableViewListResponse(
            service.getShuttleTimetable().map {
                ShuttleTimetableViewResponse(
                    seq = it.seq,
                    period = it.periodType,
                    isWeekdays = it.weekday,
                    routeName = it.routeName,
                    routeTag = it.routeTag,
                    stopName = it.stopName,
                    departureTime = it.departureTime.format(departureTimeFormatter),
                    destinationGroup = it.destinationGroup,
                )
            },
        )
    }
}
