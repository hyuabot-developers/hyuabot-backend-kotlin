package app.hyuabot.backend.bus.controller

import app.hyuabot.backend.bus.domain.BusRouteTimetableListResponse
import app.hyuabot.backend.bus.domain.BusTimetableRequest
import app.hyuabot.backend.bus.domain.BusTimetableResponse
import app.hyuabot.backend.bus.exception.BusRouteNotFoundException
import app.hyuabot.backend.bus.exception.BusStartStopNotFoundException
import app.hyuabot.backend.bus.exception.BusTimetableNotFoundException
import app.hyuabot.backend.bus.exception.DuplicateBusTimetableException
import app.hyuabot.backend.bus.service.BusTimetableService
import app.hyuabot.backend.database.entity.BusTimetable
import app.hyuabot.backend.database.exception.LocalTimeNotValidException
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import app.hyuabot.backend.utility.ResponseBuilder
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/bus/timetable")
@RestController
@Tag(name = "Bus", description = "노선 버스 관련 API")
class BusTimetableController {
    @Autowired
    private lateinit var service: BusTimetableService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("")
    @Operation(summary = "노선 버스 시간표 정보 조회", description = "노선 버스 시간표 정보를 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "노선 버스 시간표 정보 조회 성공",
        content = [Content(schema = Schema(implementation = BusRouteTimetableListResponse::class))],
    )
    fun getBusTimetable(
        @RequestParam(required = false) routeID: Int?,
        @RequestParam(required = false) startStopID: Int?,
    ): BusRouteTimetableListResponse =
        BusRouteTimetableListResponse(
            service.getBusTimetableList(routeID, startStopID).map {
                BusTimetableResponse(
                    seq = it.seq!!,
                    routeID = it.routeID,
                    startStopID = it.startStopID,
                    dayType = it.weekday,
                    departureTime = LocalDateTimeBuilder.convertLocalTimeToString(it.departureTime),
                )
            },
        )

    @PostMapping("")
    @Operation(summary = "노선 버스 시간표 정보 생성", description = "노선 버스 시간표 정보를 생성합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "노선 버스 시간표 정보 생성 성공",
                content = [Content(schema = Schema(implementation = BusTimetableResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "존재하지 않는 노선 또는 정류장",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "존재하지 않는 노선",
                                value = """{"message": "BUS_ROUTE_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "존재하지 않는 시점 정류장",
                                value = """{"message": "BUS_START_STOP_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "유효하지 않은 시간 형식",
                                value = """{"message": "INVALID_TIME_FORMAT"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "중복된 시간표",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "중복된 시간표",
                                value = """{"message": "DUPLICATE_BUS_TIMETABLE"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun createBusTimetable(
        @RequestBody payload: BusTimetableRequest,
    ): ResponseEntity<*> =
        try {
            val busTimetable: BusTimetable = service.createBusTimetable(payload)
            ResponseBuilder.response(
                HttpStatus.CREATED,
                BusTimetableResponse(
                    seq = busTimetable.seq!!,
                    routeID = busTimetable.routeID,
                    startStopID = busTimetable.startStopID,
                    dayType = busTimetable.weekday,
                    departureTime = LocalDateTimeBuilder.convertLocalTimeToString(busTimetable.departureTime),
                ),
            )
        } catch (_: LocalTimeNotValidException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("INVALID_TIME_FORMAT"),
            )
        } catch (_: BusRouteNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("BUS_ROUTE_NOT_FOUND"),
            )
        } catch (_: BusStartStopNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("BUS_START_STOP_NOT_FOUND"),
            )
        } catch (_: DuplicateBusTimetableException) {
            ResponseBuilder.response(
                HttpStatus.CONFLICT,
                ResponseBuilder.Message("DUPLICATE_BUS_TIMETABLE"),
            )
        } catch (e: Exception) {
            logger.error("Error creating bus timetable", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @GetMapping("/{seq}")
    @Operation(summary = "노선 버스 시간표 정보 조회 (단일)", description = "노선 버스 시간표 정보를 단일로 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "노선 버스 시간표 정보 조회 성공",
                content = [Content(schema = Schema(implementation = BusTimetableResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "존재하지 않는 시간표",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "존재하지 않는 시간표",
                                value = """{"message": "BUS_TIMETABLE_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getBusTimetableById(
        @PathVariable seq: Int,
    ): ResponseEntity<*> =
        try {
            val busTimetable: BusTimetable = service.getBusTimetableById(seq)
            ResponseBuilder.response(
                HttpStatus.OK,
                BusTimetableResponse(
                    seq = busTimetable.seq!!,
                    routeID = busTimetable.routeID,
                    startStopID = busTimetable.startStopID,
                    dayType = busTimetable.weekday,
                    departureTime = LocalDateTimeBuilder.convertLocalTimeToString(busTimetable.departureTime),
                ),
            )
        } catch (_: BusTimetableNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("BUS_TIMETABLE_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error fetching bus timetable by id", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PutMapping("/{seq}")
    @Operation(summary = "노선 버스 시간표 정보 수정", description = "노선 버스 시간표 정보를 수정합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "노선 버스 시간표 정보 수정 성공",
                content = [Content(schema = Schema(implementation = BusTimetableResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "존재하지 않는 노선 또는 정류장",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "존재하지 않는 노선",
                                value = """{"message": "BUS_ROUTE_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "존재하지 않는 시점 정류장",
                                value = """{"message": "BUS_START_STOP_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "유효하지 않은 시간 형식",
                                value = """{"message": "INVALID_TIME_FORMAT"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "존재하지 않는 시간표",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "존재하지 않는 시간표",
                                value = """{"message": "BUS_TIMETABLE_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "중복된 시간표",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "중복된 시간표",
                                value = """{"message": "DUPLICATE_BUS_TIMETABLE"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun updateBusTimetable(
        @PathVariable seq: Int,
        @RequestBody payload: BusTimetableRequest,
    ): ResponseEntity<*> =
        try {
            val busTimetable: BusTimetable = service.updateBusTimetable(seq, payload)
            ResponseBuilder.response(
                HttpStatus.OK,
                BusTimetableResponse(
                    seq = busTimetable.seq!!,
                    routeID = busTimetable.routeID,
                    startStopID = busTimetable.startStopID,
                    dayType = busTimetable.weekday,
                    departureTime = LocalDateTimeBuilder.convertLocalTimeToString(busTimetable.departureTime),
                ),
            )
        } catch (_: LocalTimeNotValidException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("INVALID_TIME_FORMAT"),
            )
        } catch (_: BusRouteNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("BUS_ROUTE_NOT_FOUND"),
            )
        } catch (_: BusStartStopNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("BUS_START_STOP_NOT_FOUND"),
            )
        } catch (_: BusTimetableNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("BUS_TIMETABLE_NOT_FOUND"),
            )
        } catch (_: DuplicateBusTimetableException) {
            ResponseBuilder.response(
                HttpStatus.CONFLICT,
                ResponseBuilder.Message("DUPLICATE_BUS_TIMETABLE"),
            )
        } catch (e: Exception) {
            logger.error("Error updating bus timetable", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @DeleteMapping("/{seq}")
    @Operation(summary = "노선 버스 시간표 정보 삭제", description = "노선 버스 시간표 정보를 삭제합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "노선 버스 시간표 정보 삭제 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "존재하지 않는 시간표",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "존재하지 않는 시간표",
                                value = """{"message": "BUS_TIMETABLE_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun deleteBusTimetableById(
        @PathVariable seq: Int,
    ): ResponseEntity<*> =
        try {
            service.deleteBusTimetableById(seq)
            ResponseBuilder.response(
                HttpStatus.NO_CONTENT,
                null,
            )
        } catch (_: BusTimetableNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("BUS_TIMETABLE_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error deleting bus timetable", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
}
