package app.hyuabot.backend.subway.controller

import app.hyuabot.backend.subway.domain.SubwayTimetableListResponse
import app.hyuabot.backend.subway.domain.SubwayTimetableResponse
import app.hyuabot.backend.subway.service.SubwayService
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/subway/timetable")
@RestController
@Tag(name = "Subway", description = "지하철 관련 API")
class SubwayTimetableController {
    @Autowired private lateinit var service: SubwayService

    @GetMapping("")
    @Operation(summary = "지하철 시간표 정보 조회", description = "지하철 시간표 정보를 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "지하철 시간표 정보 조회 성공",
        content = [Content(schema = Schema(implementation = SubwayTimetableListResponse::class))],
    )
    fun getSubwayTimetable(): SubwayTimetableListResponse =
        SubwayTimetableListResponse(
            service.getAllTimetables().map {
                SubwayTimetableResponse(
                    seq = it.seq!!,
                    stationID = it.stationID,
                    startStationID = it.startStationID,
                    terminalStationID = it.terminalStationID,
                    departureTime = LocalDateTimeBuilder.convertLocalTimeToString(it.departureTime),
                    weekday = it.weekday,
                    direction = it.heading,
                )
            },
        )
}
