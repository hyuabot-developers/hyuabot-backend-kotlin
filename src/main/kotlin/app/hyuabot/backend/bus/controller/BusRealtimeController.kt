package app.hyuabot.backend.bus.controller

import app.hyuabot.backend.bus.domain.BusRealtimeListResponse
import app.hyuabot.backend.bus.domain.BusRealtimeResponse
import app.hyuabot.backend.bus.service.BusRealtimeService
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

@RequestMapping("/api/v1/bus/realtime")
@RestController
@Tag(name = "Bus", description = "노선 버스 관련 API")
class BusRealtimeController {
    @Autowired private lateinit var service: BusRealtimeService

    @GetMapping("")
    @Operation(summary = "노선 버스 실시간 도착 정보 조회", description = "노선 버스 실시간 도착 정보를 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "노선 버스 실시간 도착 정보 조회 성공",
        content = [Content(schema = Schema(implementation = BusRealtimeListResponse::class))],
    )
    fun getBusRealtime(): BusRealtimeListResponse =
        BusRealtimeListResponse(
            service.getBusRealtimeList().map {
                BusRealtimeResponse(
                    routeID = it.routeID,
                    stopID = it.stopID,
                    order = it.order,
                    stop = it.remainingStop,
                    time = LocalDateTimeBuilder.convertDurationToString(it.remainingTime),
                    seat = it.remainingSeat,
                    isLowFloor = it.isLowFloor,
                    updatedAt =
                        LocalDateTimeBuilder.convertLocalDateTimeToString(
                            LocalDateTimeBuilder.convertAsServiceTimezone(it.updatedAt),
                        ),
                )
            },
        )
}
