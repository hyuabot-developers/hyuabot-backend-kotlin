package app.hyuabot.backend.bus.controller

import app.hyuabot.backend.bus.domain.BusDepartureLogListResponse
import app.hyuabot.backend.bus.domain.BusDepartureLogResponse
import app.hyuabot.backend.bus.domain.BusRouteListResponse
import app.hyuabot.backend.bus.domain.BusRouteResponse
import app.hyuabot.backend.bus.domain.BusRouteStopListResponse
import app.hyuabot.backend.bus.domain.BusRouteStopRequest
import app.hyuabot.backend.bus.domain.BusRouteStopResponse
import app.hyuabot.backend.bus.domain.CreateBusRouteRequest
import app.hyuabot.backend.bus.domain.UpdateBusRouteRequest
import app.hyuabot.backend.bus.exception.BusEndStopNotFoundException
import app.hyuabot.backend.bus.exception.BusRouteNotFoundException
import app.hyuabot.backend.bus.exception.BusRouteStopNotFoundException
import app.hyuabot.backend.bus.exception.BusStartStopNotFoundException
import app.hyuabot.backend.bus.exception.BusStopNotFoundException
import app.hyuabot.backend.bus.exception.DuplicateBusRouteException
import app.hyuabot.backend.bus.exception.DuplicateBusRouteStopException
import app.hyuabot.backend.bus.service.BusRouteService
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

@RequestMapping("/api/v1/bus/route")
@RestController
@Tag(name = "Bus", description = "노선 버스 관련 API")
class BusRouteController {
    @Autowired private lateinit var service: BusRouteService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("")
    @Operation(summary = "버스 노선 목록 조회", description = "등록된 모든 버스 노선 목록을 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "버스 노선 목록 조회 성공",
        content = [Content(schema = Schema(implementation = BusRouteListResponse::class))],
    )
    fun getAllBusRoutes(): BusRouteListResponse =
        BusRouteListResponse(
            service.getBusRouteList().map {
                BusRouteResponse(
                    id = it.id,
                    name = it.name,
                    typeCode = it.typeCode,
                    typeName = it.typeName,
                    startStopID = it.startStopID,
                    endStopID = it.endStopID,
                    upFirstTime = LocalDateTimeBuilder.convertLocalTimeToString(it.upFirstTime),
                    upLastTime = LocalDateTimeBuilder.convertLocalTimeToString(it.upLastTime),
                    downFirstTime = LocalDateTimeBuilder.convertLocalTimeToString(it.downFirstTime),
                    downLastTime = LocalDateTimeBuilder.convertLocalTimeToString(it.downLastTime),
                    districtCode = it.districtCode,
                    companyID = it.companyID,
                    companyName = it.companyName,
                    companyPhone = it.companyPhone,
                )
            },
        )

    @PostMapping("")
    @Operation(summary = "버스 노선 등록", description = "새로운 버스 노선을 등록합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "버스 노선 등록 성공",
                content = [Content(schema = Schema(implementation = BusRouteResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "버스 노선 ID 중복",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [ExampleObject(value = """{"message": "DUPLICATE_BUS_ROUTE"}""")],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description =
                    "버스 시점 정류장 ID 또는 종점 정류장 ID가 존재하지 않음\n" +
                        "버스 첫차/막차 시간 형식이 올바르지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                summary = "시점 정류장 ID가 존재하지 않음",
                                value = """{"message": "BUS_START_STOP_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                summary = "종점 정류장 ID가 존재하지 않음",
                                value = """{"message": "BUS_END_STOP_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                summary = "버스 첫차/막차 시간 형식이 올바르지 않음",
                                value = """{"message": "LOCAL_TIME_NOT_VALID"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun createBusRoute(
        @RequestBody payload: CreateBusRouteRequest,
    ): ResponseEntity<*> =
        try {
            service.createBusRoute(payload).let { busRoute ->
                ResponseBuilder.response(
                    HttpStatus.CREATED,
                    BusRouteResponse(
                        id = busRoute.id,
                        name = busRoute.name,
                        typeCode = busRoute.typeCode,
                        typeName = busRoute.typeName,
                        startStopID = busRoute.startStopID,
                        endStopID = busRoute.endStopID,
                        upFirstTime = LocalDateTimeBuilder.convertLocalTimeToString(busRoute.upFirstTime),
                        upLastTime = LocalDateTimeBuilder.convertLocalTimeToString(busRoute.upLastTime),
                        downFirstTime = LocalDateTimeBuilder.convertLocalTimeToString(busRoute.downFirstTime),
                        downLastTime = LocalDateTimeBuilder.convertLocalTimeToString(busRoute.downLastTime),
                        districtCode = busRoute.districtCode,
                        companyID = busRoute.companyID,
                        companyName = busRoute.companyName,
                        companyPhone = busRoute.companyPhone,
                    ),
                )
            }
        } catch (_: LocalTimeNotValidException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("LOCAL_TIME_NOT_VALID"),
            )
        } catch (_: BusStartStopNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("BUS_START_STOP_NOT_FOUND"),
            )
        } catch (_: BusEndStopNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("BUS_END_STOP_NOT_FOUND"),
            )
        } catch (_: DuplicateBusRouteException) {
            ResponseBuilder.response(
                HttpStatus.CONFLICT,
                ResponseBuilder.Message("DUPLICATE_BUS_ROUTE"),
            )
        } catch (e: Exception) {
            logger.error("Error creating bus route", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @GetMapping("/{id}")
    @Operation(summary = "버스 노선 조회", description = "버스 노선 ID로 버스 노선 정보를 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "버스 노선 조회 성공",
                content = [Content(schema = Schema(implementation = BusRouteResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "버스 노선 ID가 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [ExampleObject(value = """{"message": "BUS_ROUTE_NOT_FOUND"}""")],
                    ),
                ],
            ),
        ],
    )
    fun getBusRouteById(
        @PathVariable id: Int,
    ): ResponseEntity<*> =
        try {
            val busRoute = service.getBusRouteById(id)
            ResponseEntity(
                BusRouteResponse(
                    id = busRoute.id,
                    name = busRoute.name,
                    typeCode = busRoute.typeCode,
                    typeName = busRoute.typeName,
                    startStopID = busRoute.startStopID,
                    endStopID = busRoute.endStopID,
                    upFirstTime = LocalDateTimeBuilder.convertLocalTimeToString(busRoute.upFirstTime),
                    upLastTime = LocalDateTimeBuilder.convertLocalTimeToString(busRoute.upLastTime),
                    downFirstTime = LocalDateTimeBuilder.convertLocalTimeToString(busRoute.downFirstTime),
                    downLastTime = LocalDateTimeBuilder.convertLocalTimeToString(busRoute.downLastTime),
                    districtCode = busRoute.districtCode,
                    companyID = busRoute.companyID,
                    companyName = busRoute.companyName,
                    companyPhone = busRoute.companyPhone,
                ),
                HttpStatus.OK,
            )
        } catch (_: BusRouteNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("BUS_ROUTE_NOT_FOUND"),
            )
        } catch (_: Exception) {
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PutMapping("/{id}")
    @Operation(summary = "버스 노선 수정", description = "버스 노선 정보를 수정합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "버스 노선 수정 성공",
                content = [Content(schema = Schema(implementation = BusRouteResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description =
                    "버스 노선 ID가 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(value = """{"message": "BUS_ROUTE_NOT_FOUND"}"""),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "버스 첫차/막차 시간 형식이 올바르지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                summary = "시점 정류장 ID가 존재하지 않음",
                                value = """{"message": "BUS_START_STOP_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                summary = "종점 정류장 ID가 존재하지 않음",
                                value = """{"message": "BUS_END_STOP_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                summary = "버스 첫차/막차 시간 형식이 올바르지 않음",
                                value = """{"message": "LOCAL_TIME_NOT_VALID"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun updateBusRoute(
        @PathVariable id: Int,
        @RequestBody payload: UpdateBusRouteRequest,
    ): ResponseEntity<*> =
        try {
            val busRoute = service.updateBusRoute(id, payload)
            ResponseEntity(
                BusRouteResponse(
                    id = busRoute.id,
                    name = busRoute.name,
                    typeCode = busRoute.typeCode,
                    typeName = busRoute.typeName,
                    startStopID = busRoute.startStopID,
                    endStopID = busRoute.endStopID,
                    upFirstTime = LocalDateTimeBuilder.convertLocalTimeToString(busRoute.upFirstTime),
                    upLastTime = LocalDateTimeBuilder.convertLocalTimeToString(busRoute.upLastTime),
                    downFirstTime = LocalDateTimeBuilder.convertLocalTimeToString(busRoute.downFirstTime),
                    downLastTime = LocalDateTimeBuilder.convertLocalTimeToString(busRoute.downLastTime),
                    districtCode = busRoute.districtCode,
                    companyID = busRoute.companyID,
                    companyName = busRoute.companyName,
                    companyPhone = busRoute.companyPhone,
                ),
                HttpStatus.OK,
            )
        } catch (_: BusRouteNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("BUS_ROUTE_NOT_FOUND"),
            )
        } catch (_: BusStartStopNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("BUS_START_STOP_NOT_FOUND"),
            )
        } catch (_: BusEndStopNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("BUS_END_STOP_NOT_FOUND"),
            )
        } catch (_: LocalTimeNotValidException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("LOCAL_TIME_NOT_VALID"),
            )
        } catch (e: Exception) {
            logger.error("Error updating bus route", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @DeleteMapping("/{id}")
    @Operation(summary = "버스 노선 삭제", description = "버스 노선 정보를 삭제합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "버스 노선 삭제 성공",
                content = [Content(schema = Schema(implementation = ResponseBuilder.Message::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "버스 노선 ID가 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [ExampleObject(value = """{"message": "BUS_ROUTE_NOT_FOUND"}""")],
                    ),
                ],
            ),
        ],
    )
    fun deleteBusRoute(
        @PathVariable id: Int,
    ): ResponseEntity<*> =
        try {
            service.deleteBusRouteById(id)
            ResponseBuilder.response(
                HttpStatus.NO_CONTENT,
                null,
            )
        } catch (_: BusRouteNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("BUS_ROUTE_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error deleting bus route", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @GetMapping("/{id}/stop")
    @Operation(summary = "버스 노선 정류장 목록 조회", description = "버스 노선 ID로 버스 노선의 정류장 목록을 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "버스 노선 정류장 목록 조회 성공",
                content = [Content(schema = Schema(implementation = BusRouteStopResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "버스 노선 ID가 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [ExampleObject(value = """{"message": "BUS_ROUTE_NOT_FOUND"}""")],
                    ),
                ],
            ),
        ],
    )
    fun getBusRouteStopsByRouteId(
        @PathVariable id: Int,
    ): ResponseEntity<*> =
        try {
            val busRouteStops = service.getBusStopListByRouteID(id)
            ResponseEntity(
                BusRouteStopListResponse(
                    result =
                        busRouteStops.map {
                            BusRouteStopResponse(
                                seq = it.seq!!,
                                routeID = it.routeID,
                                stopID = it.stopID,
                                order = it.order,
                                startStopID = it.startStopID,
                                travelTime = it.minuteFromStart,
                            )
                        },
                ),
                HttpStatus.OK,
            )
        } catch (_: BusRouteNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("BUS_ROUTE_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error getting bus route stops", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PostMapping("/{id}/stop")
    @Operation(summary = "버스 노선 정류장 등록", description = "버스 노선에 새로운 정류장을 등록합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "버스 노선 정류장 등록 성공",
                content = [Content(schema = Schema(implementation = BusRouteStopResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description =
                    "버스 노선 ID가 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                summary = "버스 노선 ID가 존재하지 않음",
                                value = """{"message": "BUS_ROUTE_NOT_FOUND"}""",
                            ),

                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "버스 정류장 ID 또는 시점 정류장 ID가 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                summary = "버스 정류장 ID가 존재하지 않음",
                                value = """{"message": "BUS_STOP_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                summary = "시점 정류장 ID가 존재하지 않음",
                                value = """{"message": "BUS_START_STOP_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "해당 노선에 이미 동일한 순서의 정류장이 존재함",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [ExampleObject(value = """{"message": "DUPLICATE_BUS_ROUTE_STOP"}""")],
                    ),
                ],
            ),
        ],
    )
    fun addBusRouteStop(
        @PathVariable id: Int,
        @RequestBody payload: BusRouteStopRequest,
    ): ResponseEntity<*> =
        try {
            val busRouteStop = service.createBusRouteStop(id, payload)
            ResponseEntity(
                BusRouteStopResponse(
                    seq = busRouteStop.seq!!,
                    routeID = busRouteStop.routeID,
                    stopID = busRouteStop.stopID,
                    order = busRouteStop.order,
                    startStopID = busRouteStop.startStopID,
                    travelTime = busRouteStop.minuteFromStart,
                ),
                HttpStatus.CREATED,
            )
        } catch (_: BusRouteNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("BUS_ROUTE_NOT_FOUND"),
            )
        } catch (_: BusStopNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("BUS_STOP_NOT_FOUND"),
            )
        } catch (_: BusStartStopNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("BUS_START_STOP_NOT_FOUND"),
            )
        } catch (_: DuplicateBusRouteStopException) {
            ResponseBuilder.response(
                HttpStatus.CONFLICT,
                ResponseBuilder.Message("DUPLICATE_BUS_ROUTE_STOP"),
            )
        } catch (e: Exception) {
            logger.error("Error creating bus route", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PutMapping("/{id}/stop/{seq}")
    @Operation(summary = "버스 노선 정류장 수정", description = "버스 노선의 정류장 정보를 수정합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "버스 노선 정류장 수정 성공",
                content = [Content(schema = Schema(implementation = BusRouteStopResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description =
                    "버스 노선 ID 혹은 버스 노선 정류장 ID가 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                summary = "버스 노선 ID가 존재하지 않음",
                                value = """{"message": "BUS_ROUTE_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                summary = "버스 노선 정류장 ID가 존재하지 않음",
                                value = """{"message": "BUS_ROUTE_STOP_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "버스 정류장 ID 또는 시점 정류장 ID가 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                summary = "버스 정류장 ID가 존재하지 않음",
                                value = """{"message": "BUS_STOP_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                summary = "시점 정류장 ID가 존재하지 않음",
                                value = """{"message": "BUS_START_STOP_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "해당 노선에 이미 동일한 순서의 정류장이 존재함",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [ExampleObject(value = """{"message": "DUPLICATE_BUS_ROUTE_STOP"}""")],
                    ),
                ],
            ),
        ],
    )
    fun updateBusRouteStop(
        @PathVariable id: Int,
        @PathVariable seq: Int,
        @RequestBody payload: BusRouteStopRequest,
    ): ResponseEntity<*> =
        try {
            val busRouteStop = service.updateBusRouteStop(id, seq, payload)
            ResponseEntity(
                BusRouteStopResponse(
                    seq = busRouteStop.seq!!,
                    routeID = busRouteStop.routeID,
                    stopID = busRouteStop.stopID,
                    order = busRouteStop.order,
                    startStopID = busRouteStop.startStopID,
                    travelTime = busRouteStop.minuteFromStart,
                ),
                HttpStatus.OK,
            )
        } catch (_: BusRouteNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("BUS_ROUTE_NOT_FOUND"),
            )
        } catch (_: BusRouteStopNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("BUS_ROUTE_STOP_NOT_FOUND"),
            )
        } catch (_: BusStopNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("BUS_STOP_NOT_FOUND"),
            )
        } catch (_: BusStartStopNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("BUS_START_STOP_NOT_FOUND"),
            )
        } catch (_: DuplicateBusRouteStopException) {
            ResponseBuilder.response(
                HttpStatus.CONFLICT,
                ResponseBuilder.Message("DUPLICATE_BUS_ROUTE_STOP"),
            )
        } catch (e: Exception) {
            logger.error("Error updating bus route stop", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @DeleteMapping("/{id}/stop/{seq}")
    @Operation(summary = "버스 노선 정류장 삭제", description = "버스 노선의 정류장 정보를 삭제합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "버스 노선 정류장 삭제 성공",
                content = [Content(schema = Schema(implementation = ResponseBuilder.Message::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description =
                    "버스 노선 ID 혹은 버스 노선 정류장 ID가 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                summary = "버스 노선 ID가 존재하지 않음",
                                value = """{"message": "BUS_ROUTE_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                summary = "버스 노선 정류장 ID가 존재하지 않음",
                                value = """{"message": "BUS_ROUTE_STOP_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun deleteBusRouteStop(
        @PathVariable id: Int,
        @PathVariable seq: Int,
    ): ResponseEntity<*> =
        try {
            service.deleteBusRouteStopBySeq(id, seq)
            ResponseBuilder.response(
                HttpStatus.NO_CONTENT,
                null,
            )
        } catch (_: BusRouteNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("BUS_ROUTE_NOT_FOUND"),
            )
        } catch (_: BusRouteStopNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("BUS_ROUTE_STOP_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error deleting bus route stop", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @GetMapping("/{id}/stop/{seq}/log")
    @Operation(summary = "버스 노선 정류장 로그 조회", description = "버스 노선 정류장의 로그를 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "버스 노선 정류장 로그 조회 성공",
                content = [Content(schema = Schema(implementation = BusDepartureLogResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description =
                    "버스 노선 ID 혹은 버스 노선 정류장 ID가 존재하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                summary = "버스 노선 ID가 존재하지 않음",
                                value = """{"message": "BUS_ROUTE_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                summary = "버스 노선 정류장 ID가 존재하지 않음",
                                value = """{"message": "BUS_ROUTE_STOP_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getBusRouteStopLogs(
        @PathVariable id: Int,
        @PathVariable seq: Int,
    ): ResponseEntity<*> =
        try {
            val logs = service.getBusDepartureLogByRouteStop(id, seq)
            ResponseBuilder.response(
                HttpStatus.OK,
                BusDepartureLogListResponse(
                    logs.map {
                        BusDepartureLogResponse(
                            routeID = it.routeID,
                            stopID = it.stopID,
                            date = LocalDateTimeBuilder.convertLocalDateToString(it.departureDate),
                            time = LocalDateTimeBuilder.convertLocalTimeToString(it.departureTime),
                            vehicleID = it.vehicleID,
                        )
                    },
                ),
            )
        } catch (_: BusRouteNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("BUS_ROUTE_NOT_FOUND"),
            )
        } catch (_: BusRouteStopNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("BUS_ROUTE_STOP_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error getting bus route stop logs", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
}
