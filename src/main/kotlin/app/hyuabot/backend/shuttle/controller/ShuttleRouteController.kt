package app.hyuabot.backend.shuttle.controller

import app.hyuabot.backend.database.exception.DurationNotValidException
import app.hyuabot.backend.shuttle.domain.CreateShuttleRouteRequest
import app.hyuabot.backend.shuttle.domain.CreateShuttleRouteStopRequest
import app.hyuabot.backend.shuttle.domain.ShuttleRouteListResponse
import app.hyuabot.backend.shuttle.domain.ShuttleRouteResponse
import app.hyuabot.backend.shuttle.domain.ShuttleRouteStopListResponse
import app.hyuabot.backend.shuttle.domain.ShuttleRouteStopResponse
import app.hyuabot.backend.shuttle.domain.ShuttleTimetableListResponse
import app.hyuabot.backend.shuttle.domain.ShuttleTimetableRequest
import app.hyuabot.backend.shuttle.domain.ShuttleTimetableResponse
import app.hyuabot.backend.shuttle.domain.UpdateShuttleRouteRequest
import app.hyuabot.backend.shuttle.domain.UpdateShuttleRouteStopRequest
import app.hyuabot.backend.shuttle.exception.DuplicateShuttleRouteException
import app.hyuabot.backend.shuttle.exception.DuplicateShuttleRouteStopException
import app.hyuabot.backend.shuttle.exception.DuplicateShuttleTimetableException
import app.hyuabot.backend.shuttle.exception.ShuttleRouteNotFoundException
import app.hyuabot.backend.shuttle.exception.ShuttleRouteStopNotFoundException
import app.hyuabot.backend.shuttle.exception.ShuttleTimetableNotFoundException
import app.hyuabot.backend.shuttle.service.ShuttleRouteService
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

@RequestMapping("/api/v1/shuttle/route")
@RestController
@Tag(name = "Shuttle", description = "셔틀버스 관련 API")
class ShuttleRouteController {
    @Autowired private lateinit var shuttleRouteService: ShuttleRouteService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("")
    @Operation(summary = "셔틀버스 노선 조회", description = "셔틀버스 노선 목록을 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "셔틀버스 노선 목록 조회 성공",
        content = [Content(schema = Schema(implementation = ShuttleRouteListResponse::class))],
    )
    fun getShuttleRoutes(): ShuttleRouteListResponse =
        ShuttleRouteListResponse(
            result =
                shuttleRouteService.getAllRoutes().map {
                    ShuttleRouteResponse(
                        name = it.name,
                        descriptionKorean = it.descriptionKorean,
                        descriptionEnglish = it.descriptionEnglish,
                        tag = it.tag,
                        startStopID = it.startStopID,
                        endStopID = it.endStopID,
                    )
                },
        )

    @PostMapping("")
    @Operation(
        summary = "셔틀버스 노선 생성",
        description = "새로운 셔틀버스 노선을 생성합니다.",
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "셔틀버스 노선 생성 요청",
                content = [Content(schema = Schema(implementation = CreateShuttleRouteRequest::class))],
            ),
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "셔틀버스 노선 생성 성공",
                content = [Content(schema = Schema(implementation = ShuttleRouteResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "잘못된 요청",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "중복된 셔틀버스 노선",
                                summary = "이미 존재하는 셔틀버스 노선",
                                value = """{"message": "DUPLICATE_SHUTTLE_ROUTE"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun createShuttleRoute(
        @RequestBody payload: CreateShuttleRouteRequest,
    ): ResponseEntity<*> {
        try {
            val createdRoute = shuttleRouteService.createRoute(payload)
            return ResponseBuilder.response(
                HttpStatus.CREATED,
                ShuttleRouteResponse(
                    name = createdRoute.name,
                    descriptionKorean = createdRoute.descriptionKorean,
                    descriptionEnglish = createdRoute.descriptionEnglish,
                    tag = createdRoute.tag,
                    startStopID = createdRoute.startStopID,
                    endStopID = createdRoute.endStopID,
                ),
            )
        } catch (_: DuplicateShuttleRouteException) {
            return ResponseBuilder.response(
                HttpStatus.CONFLICT,
                ResponseBuilder.Message("DUPLICATE_SHUTTLE_ROUTE"),
            )
        } catch (e: Exception) {
            logger.error("Error creating shuttle route: ${payload.name}", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @GetMapping("/{name}")
    @Operation(
        summary = "셔틀버스 노선 상세 조회",
        description = "셔틀버스 노선의 상세 정보를 조회합니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "name",
                description = "셔틀버스 노선 ID",
                required = true,
                schema = Schema(type = "string", example = "DHDD"),
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "셔틀버스 노선 상세 조회 성공",
                content = [Content(schema = Schema(implementation = ShuttleRouteResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "셔틀버스 노선이 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "셔틀버스 노선 없음",
                                summary = "셔틀버스 노선이 존재하지 않음",
                                value = """{"message": "SHUTTLE_ROUTE_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getShuttleRouteByName(
        @PathVariable name: String,
    ): ResponseEntity<*> =
        try {
            val route = shuttleRouteService.getRouteByName(name)
            ResponseBuilder.response(
                HttpStatus.OK,
                ShuttleRouteResponse(
                    name = route.name,
                    descriptionKorean = route.descriptionKorean,
                    descriptionEnglish = route.descriptionEnglish,
                    tag = route.tag,
                    startStopID = route.startStopID,
                    endStopID = route.endStopID,
                ),
            )
        } catch (_: ShuttleRouteNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_ROUTE_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error retrieving shuttle route: $name", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PutMapping("/{name}")
    @Operation(
        summary = "셔틀버스 노선 수정",
        description = "특정 셔틀버스 노선을 수정합니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "name",
                description = "셔틀버스 노선 ID",
                required = true,
                schema = Schema(type = "string", example = "DHDD"),
            ),
        ],
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "셔틀버스 노선 수정 요청",
                content = [Content(schema = Schema(implementation = UpdateShuttleRouteRequest::class))],
            ),
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "셔틀버스 노선 수정 성공",
                content = [Content(schema = Schema(implementation = ShuttleRouteResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "셔틀버스 노선이 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "셔틀버스 노선 없음",
                                summary = "셔틀버스 노선이 존재하지 않음",
                                value = """{"message": "SHUTTLE_ROUTE_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun updateShuttleRoute(
        @PathVariable name: String,
        @RequestBody payload: UpdateShuttleRouteRequest,
    ): ResponseEntity<*> =
        try {
            val updatedRoute = shuttleRouteService.updateRoute(name, payload)
            ResponseBuilder.response(
                HttpStatus.OK,
                ShuttleRouteResponse(
                    name = updatedRoute.name,
                    descriptionKorean = updatedRoute.descriptionKorean,
                    descriptionEnglish = updatedRoute.descriptionEnglish,
                    tag = updatedRoute.tag,
                    startStopID = updatedRoute.startStopID,
                    endStopID = updatedRoute.endStopID,
                ),
            )
        } catch (_: ShuttleRouteNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_ROUTE_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error updating shuttle route: $name", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @DeleteMapping("/{name}")
    @Operation(
        summary = "셔틀버스 노선 삭제",
        description = "특정 셔틀버스 노선을 삭제합니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "name",
                description = "셔틀버스 노선 ID",
                required = true,
                schema = Schema(type = "string", example = "DHDD"),
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "셔틀버스 노선 삭제 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "셔틀버스 노선이 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "셔틀버스 노선 없음",
                                summary = "셔틀버스 노선이 존재하지 않음",
                                value = """{"message": "SHUTTLE_ROUTE_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun deleteShuttleRouteByName(
        @PathVariable name: String,
    ): ResponseEntity<*> =
        try {
            shuttleRouteService.deleteRouteByName(name)
            ResponseBuilder.response(HttpStatus.NO_CONTENT, null)
        } catch (_: ShuttleRouteNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_ROUTE_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error deleting shuttle route: $name", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @GetMapping("/{routeName}/stop")
    @Operation(
        summary = "셔틀버스 노선의 정류장 조회",
        description = "특정 셔틀버스 노선에 속한 정류장 목록을 조회합니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "routeName",
                description = "셔틀버스 노선 ID",
                required = true,
                schema = Schema(type = "string", example = "DHDD"),
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "셔틀버스 노선의 정류장 목록 조회 성공",
                content = [Content(schema = Schema(implementation = ShuttleRouteStopListResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "셔틀버스 노선이 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "셔틀버스 노선 없음",
                                summary = "셔틀버스 노선이 존재하지 않음",
                                value = """{"message": "SHUTTLE_ROUTE_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getShuttleStopByRouteName(
        @PathVariable routeName: String,
    ): ResponseEntity<*> =
        try {
            val stops = shuttleRouteService.getShuttleStopByRouteName(routeName)
            ResponseBuilder.response(
                HttpStatus.OK,
                ShuttleRouteStopListResponse(
                    result =
                        stops.map {
                            ShuttleRouteStopResponse(
                                seq = it.seq!!,
                                name = it.stopName,
                                order = it.order,
                                cumulativeTime = LocalDateTimeBuilder.convertDurationToString(it.cumulativeTime),
                            )
                        },
                ),
            )
        } catch (_: ShuttleRouteNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_ROUTE_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error retrieving shuttle stops for route: $routeName", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PostMapping("/{routeName}/stop")
    @Operation(
        summary = "셔틀버스 노선에 정류장 추가",
        description = "특정 셔틀버스 노선에 정류장을 추가합니다",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "routeName",
                description = "셔틀버스 노선 ID",
                required = true,
                schema = Schema(type = "string", example = "DHDD"),
            ),
        ],
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "셔틀버스 노선 정류장 추가 요청",
                content = [Content(schema = Schema(implementation = CreateShuttleRouteStopRequest::class))],
            ),
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "셔틀버스 노선 정류장 추가 성공",
                content = [Content(schema = Schema(implementation = ShuttleRouteStopResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 형식",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "잘못된 요청 형식",
                                summary = "날짜/시간 형식이 잘못됨",
                                value = """{"message": "INVALID_DATE_TIME_FORMAT"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "셔틀버스 노선 및 정류장이 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "셔틀버스 노선 없음",
                                summary = "셔틀버스 노선이 존재하지 않음",
                                value = """{"message": "SHUTTLE_ROUTE_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "셔틀버스 정류장 없음",
                                summary = "셔틀버스 정류장이 존재하지 않음",
                                value = """{"message": "SHUTTLE_ROUTE_STOP_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "정류장이 이미 존재함",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "중복된 셔틀버스 노선 - 정류장",
                                summary = "이미 존재하는 셔틀버스 노선 및 정류장",
                                value = """{"message": "DUPLICATE_SHUTTLE_ROUTE_STOP"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun createShuttleRouteStop(
        @PathVariable routeName: String,
        @RequestBody payload: CreateShuttleRouteStopRequest,
    ): ResponseEntity<*> =
        try {
            val createdStop = shuttleRouteService.createShuttleRouteStop(routeName, payload)
            ResponseBuilder.response(
                HttpStatus.CREATED,
                ShuttleRouteStopResponse(
                    seq = createdStop.seq!!,
                    name = createdStop.stopName,
                    order = createdStop.order,
                    cumulativeTime = LocalDateTimeBuilder.convertDurationToString(createdStop.cumulativeTime),
                ),
            )
        } catch (_: ShuttleRouteNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_ROUTE_NOT_FOUND"),
            )
        } catch (_: ShuttleRouteStopNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_ROUTE_STOP_NOT_FOUND"),
            )
        } catch (_: DuplicateShuttleRouteStopException) {
            ResponseBuilder.response(
                HttpStatus.CONFLICT,
                ResponseBuilder.Message("DUPLICATE_SHUTTLE_ROUTE_STOP"),
            )
        } catch (_: DurationNotValidException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("INVALID_DATE_TIME_FORMAT"),
            )
        } catch (e: Exception) {
            logger.error("Error creating shuttle route stop for route: $routeName", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PutMapping("/{routeName}/stop/{stopName}")
    @Operation(
        summary = "셔틀버스 노선의 정류장 수정",
        description = "특정 셔틀버스 노선의 정류장을 수정합니다",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "routeName",
                description = "셔틀버스 노선 ID",
                required = true,
                schema = Schema(type = "string", example = "DHDD"),
            ),
            io.swagger.v3.oas.annotations.Parameter(
                name = "stopName",
                description = "셔틀버스 정류장 ID",
                required = true,
                schema = Schema(type = "string", example = "dormitory_o"),
            ),
        ],
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "셔틀버스 노선 정류장 수정 요청",
                content = [Content(schema = Schema(implementation = UpdateShuttleRouteStopRequest::class))],
            ),
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "셔틀버스 노선 정류장 수정 성공",
                content = [Content(schema = Schema(implementation = ShuttleRouteStopResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 형식",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "잘못된 요청 형식",
                                summary = "날짜/시간 형식이 잘못됨",
                                value = """{"message": "INVALID_DATE_TIME_FORMAT"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "셔틀버스 노선 또는 정류장이 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "셔틀버스 노선 없음",
                                summary = "셔틀버스 노선이 존재하지 않음",
                                value = """{"message": "SHUTTLE_ROUTE_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "셔틀버스 정류장 없음",
                                summary = "셔틀버스 정류장이 존재하지 않음",
                                value = """{"message": "SHUTTLE_ROUTE_STOP_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun updateShuttleRouteStop(
        @PathVariable routeName: String,
        @PathVariable stopName: String,
        @RequestBody payload: UpdateShuttleRouteStopRequest,
    ): ResponseEntity<*> =
        try {
            val updatedStop = shuttleRouteService.updateShuttleRouteStop(routeName, stopName, payload)
            ResponseBuilder.response(
                HttpStatus.OK,
                ShuttleRouteStopResponse(
                    seq = updatedStop.seq!!,
                    name = updatedStop.stopName,
                    order = updatedStop.order,
                    cumulativeTime = LocalDateTimeBuilder.convertDurationToString(updatedStop.cumulativeTime),
                ),
            )
        } catch (_: ShuttleRouteNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_ROUTE_NOT_FOUND"),
            )
        } catch (_: ShuttleRouteStopNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_ROUTE_STOP_NOT_FOUND"),
            )
        } catch (_: DurationNotValidException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("INVALID_DATE_TIME_FORMAT"),
            )
        } catch (e: Exception) {
            logger.error("Error updating shuttle route stop for route: $routeName, stop: $stopName", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @DeleteMapping("/{routeName}/stop/{stopName}")
    @Operation(
        summary = "셔틀버스 노선의 정류장 삭제",
        description = "특정 셔틀버스 노선의 정류장을 삭제합니다",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "routeName",
                description = "셔틀버스 노선 ID",
                required = true,
                schema = Schema(type = "string", example = "DHDD"),
            ),
            io.swagger.v3.oas.annotations.Parameter(
                name = "stopName",
                description = "셔틀버스 정류장 ID",
                required = true,
                schema = Schema(type = "string", example = "dormitory_o"),
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "셔틀버스 노선 정류장 삭제 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "셔틀버스 노선 또는 정류장이 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "셔틀버스 노선 없음",
                                summary = "셔틀버스 노선이 존재하지 않음",
                                value = """{"message": "SHUTTLE_ROUTE_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "셔틀버스 정류장 없음",
                                summary = "셔틀버스 정류장이 존재하지 않음",
                                value = """{"message": "SHUTTLE_ROUTE_STOP_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun deleteShuttleRouteStop(
        @PathVariable routeName: String,
        @PathVariable stopName: String,
    ): ResponseEntity<*> =
        try {
            shuttleRouteService.deleteShuttleRouteStop(routeName, stopName)
            ResponseBuilder.response(HttpStatus.NO_CONTENT, null)
        } catch (_: ShuttleRouteNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_ROUTE_NOT_FOUND"),
            )
        } catch (_: ShuttleRouteStopNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_ROUTE_STOP_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error deleting shuttle route stop for route: $routeName, stop: $stopName", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @GetMapping("/{routeName}/timetable")
    @Operation(
        summary = "셔틀버스 노선의 시간표 조회",
        description = "특정 셔틀버스 노선의 시간표를 조회합니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "routeName",
                description = "셔틀버스 노선 ID",
                required = true,
                schema = Schema(type = "string", example = "DHDD"),
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "셔틀버스 노선 시간표 조회 성공",
                content = [Content(schema = Schema(implementation = ShuttleTimetableListResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "셔틀버스 노선이 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "셔틀버스 노선 없음",
                                summary = "셔틀버스 노선이 존재하지 않음",
                                value = """{"message": "SHUTTLE_ROUTE_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getShuttleTimetableByRouteName(
        @PathVariable routeName: String,
        @RequestParam(required = false) period: String?,
        @RequestParam(required = false) isWeekdays: Boolean?,
    ): ResponseEntity<*> =
        try {
            val timetables =
                shuttleRouteService.getShuttleTimetableByRouteName(
                    routeName = routeName,
                    period = period,
                    isWeekdays = isWeekdays,
                )
            ResponseBuilder.response(
                HttpStatus.OK,
                ShuttleTimetableListResponse(
                    result =
                        timetables.map {
                            ShuttleTimetableResponse(
                                seq = it.seq!!,
                                period = it.periodType,
                                isWeekdays = it.weekday,
                                departureTime = LocalDateTimeBuilder.convertLocalTimeToString(it.departureTime),
                            )
                        },
                ),
            )
        } catch (_: ShuttleRouteNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_ROUTE_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error retrieving shuttle timetable for route: $routeName", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PostMapping("/{routeName}/timetable")
    @Operation(
        summary = "셔틀버스 노선의 시간표 생성",
        description = "특정 셔틀버스 노선의 시간표를 생성합니다",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "routeName",
                description = "셔틀버스 노선 ID",
                required = true,
                schema = Schema(type = "string", example = "DHDD"),
            ),
        ],
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "셔틀버스 노선 시간표 생성 요청",
                content = [Content(schema = Schema(implementation = ShuttleTimetableRequest::class))],
            ),
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "셔틀버스 노선 시간표 생성 성공",
                content = [Content(schema = Schema(implementation = ShuttleTimetableResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 형식",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "잘못된 요청 형식",
                                summary = "날짜/시간 형식이 잘못됨",
                                value = """{"message": "INVALID_DATE_TIME_FORMAT"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "중복된 셔틀버스 시간표",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "중복된 셔틀버스 시간표",
                                summary = "이미 존재하는 셔틀버스 시간표",
                                value = """{"message": "DUPLICATE_SHUTTLE_TIMETABLE"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "셔틀버스 노선이 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "셔틀버스 노선 없음",
                                summary = "셔틀버스 노선이 존재하지 않음",
                                value = """{"message": "SHUTTLE_ROUTE_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun createShuttleTimetable(
        @PathVariable routeName: String,
        @RequestBody payload: ShuttleTimetableRequest,
    ): ResponseEntity<*> {
        try {
            val createdTimetable = shuttleRouteService.createShuttleTimetable(routeName, payload)
            return ResponseBuilder.response(
                HttpStatus.CREATED,
                ShuttleTimetableResponse(
                    seq = createdTimetable.seq!!,
                    period = createdTimetable.periodType,
                    isWeekdays = createdTimetable.weekday,
                    departureTime = LocalDateTimeBuilder.convertLocalTimeToString(createdTimetable.departureTime),
                ),
            )
        } catch (_: DuplicateShuttleTimetableException) {
            return ResponseBuilder.response(
                HttpStatus.CONFLICT,
                ResponseBuilder.Message("DUPLICATE_SHUTTLE_TIMETABLE"),
            )
        } catch (_: ShuttleRouteNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_ROUTE_NOT_FOUND"),
            )
        } catch (_: DurationNotValidException) {
            return ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("INVALID_DATE_TIME_FORMAT"),
            )
        } catch (e: Exception) {
            logger.error("Error creating shuttle timetable for route: $routeName", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @GetMapping("/{routeName}/timetable/{seq}")
    @Operation(
        summary = "셔틀버스 노선의 시간표 상세 조회",
        description = "특정 셔틀버스 노선의 시간표를 상세 조회합니다",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "routeName",
                description = "셔틀버스 노선 ID",
                required = true,
                schema = Schema(type = "string", example = "DHDD"),
            ),
            io.swagger.v3.oas.annotations.Parameter(
                name = "seq",
                description = "셔틀버스 시간표 ID",
                required = true,
                schema = Schema(type = "integer", example = "1"),
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "셔틀버스 노선 시간표 상세 조회 성공",
                content = [Content(schema = Schema(implementation = ShuttleTimetableResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "셔틀버스 노선 또는 시간표가 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "셔틀버스 노선 없음",
                                summary = "셔틀버스 노선이 존재하지 않음",
                                value = """{"message": "SHUTTLE_ROUTE_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "셔틀버스 시간표 없음",
                                summary = "셔틀버스 시간표가 존재하지 않음",
                                value = """{"message": "SHUTTLE_TIMETABLE_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getShuttleTimetableByRouteNameAndSeq(
        @PathVariable routeName: String,
        @PathVariable seq: Int,
    ): ResponseEntity<*> =
        try {
            val timetable = shuttleRouteService.getShuttleTimetableByRouteNameAndSeq(routeName, seq)
            ResponseBuilder.response(
                HttpStatus.OK,
                ShuttleTimetableResponse(
                    seq = timetable.seq!!,
                    period = timetable.periodType,
                    isWeekdays = timetable.weekday,
                    departureTime = LocalDateTimeBuilder.convertLocalTimeToString(timetable.departureTime),
                ),
            )
        } catch (_: ShuttleRouteNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_ROUTE_NOT_FOUND"),
            )
        } catch (_: ShuttleTimetableNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_TIMETABLE_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error retrieving shuttle timetable for route: $routeName, seq: $seq", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PutMapping("/{routeName}/timetable/{seq}")
    @Operation(
        summary = "셔틀버스 노선의 시간표 수정",
        description = "특정 셔틀버스 노선의 시간표를 수정합니다",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "routeName",
                description = "셔틀버스 노선 ID",
                required = true,
                schema = Schema(type = "string", example = "DHDD"),
            ),
            io.swagger.v3.oas.annotations.Parameter(
                name = "seq",
                description = "셔틀버스 시간표 ID",
                required = true,
                schema = Schema(type = "integer", example = "1"),
            ),
        ],
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "셔틀버스 노선 시간표 수정 요청",
                content = [Content(schema = Schema(implementation = ShuttleTimetableRequest::class))],
            ),
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "셔틀버스 노선 시간표 수정 성공",
                content = [Content(schema = Schema(implementation = ShuttleTimetableResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 형식",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "잘못된 요청 형식",
                                summary = "날짜/시간 형식이 잘못됨",
                                value = """{"message": "INVALID_DATE_TIME_FORMAT"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "셔틀버스 노선 또는 시간표가 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "셔틀버스 노선 없음",
                                summary = "셔틀버스 노선이 존재하지 않음",
                                value = """{"message": "SHUTTLE_ROUTE_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "셔틀버스 시간표 없음",
                                summary = "셔틀버스 시간표가 존재하지 않음",
                                value = """{"message": "SHUTTLE_TIMETABLE_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun updateShuttleTimetable(
        @PathVariable routeName: String,
        @PathVariable seq: Int,
        @RequestBody payload: ShuttleTimetableRequest,
    ): ResponseEntity<*> {
        try {
            val updatedTimetable = shuttleRouteService.updateShuttleTimetable(routeName, seq, payload)
            return ResponseBuilder.response(
                HttpStatus.OK,
                ShuttleTimetableResponse(
                    seq = updatedTimetable.seq!!,
                    period = updatedTimetable.periodType,
                    isWeekdays = updatedTimetable.weekday,
                    departureTime = LocalDateTimeBuilder.convertLocalTimeToString(updatedTimetable.departureTime),
                ),
            )
        } catch (_: ShuttleRouteNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_ROUTE_NOT_FOUND"),
            )
        } catch (_: ShuttleTimetableNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_TIMETABLE_NOT_FOUND"),
            )
        } catch (_: DurationNotValidException) {
            return ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("INVALID_DATE_TIME_FORMAT"),
            )
        } catch (e: Exception) {
            logger.error("Error updating shuttle timetable for route: $routeName, seq: $seq", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @DeleteMapping("/{routeName}/timetable/{seq}")
    @Operation(
        summary = "셔틀버스 노선의 시간표 삭제",
        description = "특정 셔틀버스 노선의 시간표를 삭제합니다",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "routeName",
                description = "셔틀버스 노선 ID",
                required = true,
                schema = Schema(type = "string", example = "DHDD"),
            ),
            io.swagger.v3.oas.annotations.Parameter(
                name = "seq",
                description = "셔틀버스 시간표 ID",
                required = true,
                schema = Schema(type = "integer", example = "1"),
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "셔틀버스 노선 시간표 삭제 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "셔틀버스 노선 또는 시간표가 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "셔틀버스 노선 없음",
                                summary = "셔틀버스 노선이 존재하지 않음",
                                value = """{"message": "SHUTTLE_ROUTE_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "셔틀버스 시간표 없음",
                                summary = "셔틀버스 시간표가 존재하지 않음",
                                value = """{"message": "SHUTTLE_TIMETABLE_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun deleteShuttleTimetable(
        @PathVariable routeName: String,
        @PathVariable seq: Int,
    ): ResponseEntity<*> {
        try {
            shuttleRouteService.deleteShuttleTimetable(routeName, seq)
            return ResponseBuilder.response(HttpStatus.NO_CONTENT, null)
        } catch (_: ShuttleRouteNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_ROUTE_NOT_FOUND"),
            )
        } catch (_: ShuttleTimetableNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_TIMETABLE_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error deleting shuttle timetable for route: $routeName, seq: $seq", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }
}
