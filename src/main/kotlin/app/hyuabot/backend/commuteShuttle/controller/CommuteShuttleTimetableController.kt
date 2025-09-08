package app.hyuabot.backend.commuteShuttle.controller

import app.hyuabot.backend.commuteShuttle.CommuteShuttleService
import app.hyuabot.backend.commuteShuttle.domain.ShuttleTimetableListResponse
import app.hyuabot.backend.commuteShuttle.domain.ShuttleTimetableResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.format.DateTimeFormatter

@RequestMapping("/api/v1/commute-shuttle/timetable")
@RestController
@Tag(name = "Commute Shuttle", description = "통학버스 관련 API")
class CommuteShuttleTimetableController {
    @Autowired private lateinit var service: CommuteShuttleService

    @GetMapping("")
    @Operation(summary = "통학버스 시간표 조회", description = "통학버스 시간표를 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "통학버스 시간표 조회 성공",
        content = [Content(schema = Schema(implementation = ShuttleTimetableListResponse::class))],
    )
    fun getShuttleTimetable(): ShuttleTimetableListResponse {
        val departureTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        return ShuttleTimetableListResponse(
            service.getAllTimetables().map {
                ShuttleTimetableResponse(
                    seq = it.seq!!,
                    stopID = it.stopName,
                    routeID = it.routeName,
                    order = it.order,
                    time = it.departureTime.format(departureTimeFormatter),
                )
            },
        )
    }
}
