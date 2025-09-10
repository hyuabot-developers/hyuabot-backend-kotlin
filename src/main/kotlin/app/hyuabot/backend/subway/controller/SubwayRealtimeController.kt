package app.hyuabot.backend.subway.controller

import app.hyuabot.backend.subway.domain.SubwayRealtimeListResponse
import app.hyuabot.backend.subway.domain.SubwayRealtimeResponse
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

@RequestMapping("/api/v1/subway/realtime")
@RestController
@Tag(name = "Subway", description = "지하철 관련 API")
class SubwayRealtimeController {
    @Autowired private lateinit var service: SubwayService

    @GetMapping("")
    @Operation(summary = "지하철 실시간 도착 정보 조회", description = "지하철 실시간 도착 정보를 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "지하철 실시간 도착 정보 조회 성공",
        content = [Content(schema = Schema(implementation = SubwayRealtimeListResponse::class))],
    )
    fun getSubwayRealtime(): SubwayRealtimeListResponse =
        SubwayRealtimeListResponse(
            service.getRealtimeList().map {
                SubwayRealtimeResponse(
                    stationID = it.stationID,
                    direction = it.heading,
                    order = it.order,
                    location = it.location,
                    stop = it.remainingStop,
                    time = LocalDateTimeBuilder.convertDurationToString(it.remainingTime),
                    terminalStationID = it.terminalStationID,
                    trainNumber = it.trainNumber,
                    updateTime =
                        LocalDateTimeBuilder.convertLocalDateTimeToString(
                            LocalDateTimeBuilder.convertAsServiceTimezone(it.updatedAt),
                        ),
                    isExpress = it.isExpress,
                    isLast = it.isLast,
                    status = it.status,
                )
            },
        )
}
