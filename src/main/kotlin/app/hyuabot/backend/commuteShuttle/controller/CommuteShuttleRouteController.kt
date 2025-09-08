package app.hyuabot.backend.commuteShuttle.controller

import app.hyuabot.backend.commuteShuttle.CommuteShuttleService
import app.hyuabot.backend.commuteShuttle.domain.CreateShuttleRouteRequest
import app.hyuabot.backend.commuteShuttle.domain.ShuttleRouteListResponse
import app.hyuabot.backend.commuteShuttle.domain.ShuttleRouteResponse
import app.hyuabot.backend.commuteShuttle.domain.ShuttleTimetableListResponse
import app.hyuabot.backend.commuteShuttle.domain.ShuttleTimetableRequest
import app.hyuabot.backend.commuteShuttle.domain.ShuttleTimetableResponse
import app.hyuabot.backend.commuteShuttle.domain.UpdateShuttleRouteRequest
import app.hyuabot.backend.commuteShuttle.exception.DuplicateShuttleRouteException
import app.hyuabot.backend.commuteShuttle.exception.ShuttleRouteNotFoundException
import app.hyuabot.backend.commuteShuttle.exception.ShuttleStopNotFoundException
import app.hyuabot.backend.commuteShuttle.exception.ShuttleTimetableNotFoundException
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
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/commute-shuttle/route")
@RestController
@Tag(name = "Commute Shuttle", description = "통학버스 관련 API")
class CommuteShuttleRouteController {
    @Autowired private lateinit var service: CommuteShuttleService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("")
    @Operation(summary = "통학버스 노선 조회", description = "통학버스 노선 목록을 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "통학버스 노선 목록 조회 성공",
        content = [Content(schema = Schema(implementation = ShuttleRouteListResponse::class))],
    )
    fun getShuttleRoutes(): ShuttleRouteListResponse =
        ShuttleRouteListResponse(
            result =
                service.getAllRoutes().map {
                    ShuttleRouteResponse(
                        name = it.name,
                        descriptionKorean = it.descriptionKorean,
                        descriptionEnglish = it.descriptionEnglish,
                    )
                },
        )

    @PostMapping("")
    @Operation(
        summary = "통학버스 노선 생성",
        description = "새로운 통학버스 노선을 생성합니다.",
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "통학버스 노선 생성 요청",
                content = [Content(schema = Schema(implementation = CreateShuttleRouteRequest::class))],
            ),
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "통학버스 노선 생성 성공",
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
                                name = "중복된 통학버스 노선",
                                summary = "이미 존재하는 통학버스 노선",
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
            val createdRoute = service.createRoute(payload)
            return ResponseBuilder.response(
                HttpStatus.CREATED,
                ShuttleRouteResponse(
                    name = createdRoute.name,
                    descriptionKorean = createdRoute.descriptionKorean,
                    descriptionEnglish = createdRoute.descriptionEnglish,
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
        summary = "통학버스 노선 상세 조회",
        description = "통학버스 노선의 상세 정보를 조회합니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "name",
                description = "통학버스 노선 ID",
                required = true,
                schema = Schema(type = "string", example = "DHDD"),
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "통학버스 노선 상세 조회 성공",
                content = [Content(schema = Schema(implementation = ShuttleRouteResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "통학버스 노선이 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "통학버스 노선 없음",
                                summary = "통학버스 노선이 존재하지 않음",
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
            val route = service.getRouteByName(name)
            ResponseBuilder.response(
                HttpStatus.OK,
                ShuttleRouteResponse(
                    name = route.name,
                    descriptionKorean = route.descriptionKorean,
                    descriptionEnglish = route.descriptionEnglish,
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
        summary = "통학버스 노선 수정",
        description = "특정 통학버스 노선을 수정합니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "name",
                description = "통학버스 노선 ID",
                required = true,
                schema = Schema(type = "string", example = "DHDD"),
            ),
        ],
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "통학버스 노선 수정 요청",
                content = [Content(schema = Schema(implementation = UpdateShuttleRouteRequest::class))],
            ),
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "통학버스 노선 수정 성공",
                content = [Content(schema = Schema(implementation = ShuttleRouteResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "통학버스 노선이 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "통학버스 노선 없음",
                                summary = "통학버스 노선이 존재하지 않음",
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
            val updatedRoute = service.updateRoute(name, payload)
            ResponseBuilder.response(
                HttpStatus.OK,
                ShuttleRouteResponse(
                    name = updatedRoute.name,
                    descriptionKorean = updatedRoute.descriptionKorean,
                    descriptionEnglish = updatedRoute.descriptionEnglish,
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
        summary = "통학버스 노선 삭제",
        description = "특정 통학버스 노선을 삭제합니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "name",
                description = "통학버스 노선 ID",
                required = true,
                schema = Schema(type = "string", example = "DHDD"),
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "통학버스 노선 삭제 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "통학버스 노선이 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "통학버스 노선 없음",
                                summary = "통학버스 노선이 존재하지 않음",
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
            service.deleteRoute(name)
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

    @GetMapping("/{routeName}/timetable")
    @Operation(
        summary = "통학버스 노선의 시간표 조회",
        description = "특정 통학버스 노선의 시간표를 조회합니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "routeName",
                description = "통학버스 노선 ID",
                required = true,
                schema = Schema(type = "string", example = "4"),
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "통학버스 노선 시간표 조회 성공",
                content = [Content(schema = Schema(implementation = ShuttleTimetableListResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "통학버스 노선이 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "통학버스 노선 없음",
                                summary = "통학버스 노선이 존재하지 않음",
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
    ): ResponseEntity<*> =
        try {
            val timetables =
                service.getTimetableByRouteName(routeName)
            ResponseBuilder.response(
                HttpStatus.OK,
                ShuttleTimetableListResponse(
                    result =
                        timetables.map {
                            ShuttleTimetableResponse(
                                seq = it.seq!!,
                                stopID = it.stopName,
                                routeID = it.routeName,
                                order = it.order,
                                time = LocalDateTimeBuilder.convertLocalTimeToString(it.departureTime),
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
        summary = "통학버스 노선의 시간표 생성",
        description = "특정 통학버스 노선의 시간표를 생성합니다",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "routeName",
                description = "통학버스 노선 ID",
                required = true,
                schema = Schema(type = "string", example = "DHDD"),
            ),
        ],
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "통학버스 노선 시간표 생성 요청",
                content = [Content(schema = Schema(implementation = ShuttleTimetableRequest::class))],
            ),
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "통학버스 노선 시간표 생성 성공",
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
                description = "통학버스 노선이 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "통학버스 노선 없음",
                                summary = "통학버스 노선이 존재하지 않음",
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
            val createdTimetable = service.createTimetable(routeName, payload)
            return ResponseBuilder.response(
                HttpStatus.CREATED,
                ShuttleTimetableResponse(
                    seq = createdTimetable.seq!!,
                    routeID = createdTimetable.routeName,
                    stopID = createdTimetable.stopName,
                    order = createdTimetable.order,
                    time = LocalDateTimeBuilder.convertLocalTimeToString(createdTimetable.departureTime),
                ),
            )
        } catch (_: ShuttleRouteNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_ROUTE_NOT_FOUND"),
            )
        } catch (_: ShuttleStopNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("SHUTTLE_STOP_NOT_FOUND"),
            )
        } catch (_: LocalTimeNotValidException) {
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
        summary = "통학버스 노선의 시간표 상세 조회",
        description = "특정 통학버스 노선의 시간표를 상세 조회합니다",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "routeName",
                description = "통학버스 노선 ID",
                required = true,
                schema = Schema(type = "string", example = "DHDD"),
            ),
            io.swagger.v3.oas.annotations.Parameter(
                name = "seq",
                description = "통학버스 시간표 ID",
                required = true,
                schema = Schema(type = "integer", example = "1"),
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "통학버스 노선 시간표 상세 조회 성공",
                content = [Content(schema = Schema(implementation = ShuttleTimetableResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "통학버스 노선 또는 시간표가 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "통학버스 노선 없음",
                                summary = "통학버스 노선이 존재하지 않음",
                                value = """{"message": "SHUTTLE_ROUTE_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "통학버스 시간표 없음",
                                summary = "통학버스 시간표가 존재하지 않음",
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
            val timetable = service.getShuttleTimetableByRouteNameAndSeq(routeName, seq)
            ResponseBuilder.response(
                HttpStatus.OK,
                ShuttleTimetableResponse(
                    seq = timetable.seq!!,
                    stopID = timetable.stopName,
                    routeID = timetable.routeName,
                    order = timetable.order,
                    time = LocalDateTimeBuilder.convertLocalTimeToString(timetable.departureTime),
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
        summary = "통학버스 노선의 시간표 수정",
        description = "특정 통학버스 노선의 시간표를 수정합니다",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "routeName",
                description = "통학버스 노선 ID",
                required = true,
                schema = Schema(type = "string", example = "DHDD"),
            ),
            io.swagger.v3.oas.annotations.Parameter(
                name = "seq",
                description = "통학버스 시간표 ID",
                required = true,
                schema = Schema(type = "integer", example = "1"),
            ),
        ],
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "통학버스 노선 시간표 수정 요청",
                content = [Content(schema = Schema(implementation = ShuttleTimetableRequest::class))],
            ),
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "통학버스 노선 시간표 수정 성공",
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
                description = "통학버스 노선 또는 시간표가 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "통학버스 노선 없음",
                                summary = "통학버스 노선이 존재하지 않음",
                                value = """{"message": "SHUTTLE_ROUTE_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "통학버스 시간표 없음",
                                summary = "통학버스 시간표가 존재하지 않음",
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
            val updatedTimetable = service.updateTimetable(routeName, seq, payload)
            return ResponseBuilder.response(
                HttpStatus.OK,
                ShuttleTimetableResponse(
                    seq = updatedTimetable.seq!!,
                    routeID = updatedTimetable.routeName,
                    stopID = updatedTimetable.stopName,
                    order = updatedTimetable.order,
                    time = LocalDateTimeBuilder.convertLocalTimeToString(updatedTimetable.departureTime),
                ),
            )
        } catch (_: ShuttleRouteNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_ROUTE_NOT_FOUND"),
            )
        } catch (_: ShuttleStopNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("SHUTTLE_STOP_NOT_FOUND"),
            )
        } catch (_: ShuttleTimetableNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_TIMETABLE_NOT_FOUND"),
            )
        } catch (_: LocalTimeNotValidException) {
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
        summary = "통학버스 노선의 시간표 삭제",
        description = "특정 통학버스 노선의 시간표를 삭제합니다",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "routeName",
                description = "통학버스 노선 ID",
                required = true,
                schema = Schema(type = "string", example = "DHDD"),
            ),
            io.swagger.v3.oas.annotations.Parameter(
                name = "seq",
                description = "통학버스 시간표 ID",
                required = true,
                schema = Schema(type = "integer", example = "1"),
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "통학버스 노선 시간표 삭제 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "통학버스 노선 또는 시간표가 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "통학버스 노선 없음",
                                summary = "통학버스 노선이 존재하지 않음",
                                value = """{"message": "SHUTTLE_ROUTE_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "통학버스 시간표 없음",
                                summary = "통학버스 시간표가 존재하지 않음",
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
            service.deleteTimetable(seq)
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
