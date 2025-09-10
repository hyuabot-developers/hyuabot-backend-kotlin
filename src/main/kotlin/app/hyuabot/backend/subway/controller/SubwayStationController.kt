package app.hyuabot.backend.subway.controller

import app.hyuabot.backend.database.exception.DurationNotValidException
import app.hyuabot.backend.database.exception.LocalTimeNotValidException
import app.hyuabot.backend.subway.domain.CreateSubwayStationRequest
import app.hyuabot.backend.subway.domain.SubwayStationListResponse
import app.hyuabot.backend.subway.domain.SubwayStationResponse
import app.hyuabot.backend.subway.domain.SubwayTimetableListResponse
import app.hyuabot.backend.subway.domain.SubwayTimetableRequest
import app.hyuabot.backend.subway.domain.SubwayTimetableResponse
import app.hyuabot.backend.subway.domain.UpdateSubwayStationRequest
import app.hyuabot.backend.subway.exception.DuplicateSubwayStationException
import app.hyuabot.backend.subway.exception.DuplicateSubwayTimetableException
import app.hyuabot.backend.subway.exception.SubwayStartStationNotFoundException
import app.hyuabot.backend.subway.exception.SubwayStationNotFoundException
import app.hyuabot.backend.subway.exception.SubwayTerminalStationNotFoundException
import app.hyuabot.backend.subway.exception.SubwayTimetableNotFoundException
import app.hyuabot.backend.subway.service.SubwayService
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

@RequestMapping("/api/v1/subway/station")
@RestController
@Tag(name = "Subway", description = "지하철 관련 API")
class SubwayStationController {
    @Autowired private lateinit var service: SubwayService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("")
    @Operation(summary = "지하철 역 정보 조회", description = "지하철 역 정보를 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "지하철 역 정보 조회 성공",
        content = [Content(schema = Schema(implementation = SubwayStationListResponse::class))],
    )
    fun getSubwayStation(): SubwayStationListResponse =
        SubwayStationListResponse(
            service.getAllStations().map {
                SubwayStationResponse(
                    id = it.id,
                    name = it.name,
                    routeID = it.routeID,
                    order = it.order,
                    cumulativeTime = LocalDateTimeBuilder.convertDurationToString(it.cumulativeTime),
                )
            },
        )

    @PostMapping("")
    @Operation(summary = "지하철 역 생성", description = "새로운 지하철 역을 생성합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "지하철 역 생성 성공",
                content = [Content(schema = Schema(implementation = SubwayStationResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "잘못된 시간 형식",
                                value = """{"message": "INVALID_TIME_FORMAT"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "중복된 지하철 역 ID",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "중복된 지하철 역 ID",
                                value = """{"message": "DUPLICATED_STATION_ID"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun createSubwayStation(
        @RequestBody payload: CreateSubwayStationRequest,
    ): ResponseEntity<*> {
        try {
            return ResponseBuilder.response(
                HttpStatus.CREATED,
                service.createStation(payload).let {
                    SubwayStationResponse(
                        id = it.id,
                        name = it.name,
                        routeID = it.routeID,
                        order = it.order,
                        cumulativeTime = LocalDateTimeBuilder.convertDurationToString(it.cumulativeTime),
                    )
                },
            )
        } catch (_: DurationNotValidException) {
            return ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("INVALID_TIME_FORMAT"),
            )
        } catch (_: DuplicateSubwayStationException) {
            return ResponseBuilder.response(
                HttpStatus.CONFLICT,
                ResponseBuilder.Message("DUPLICATED_STATION_ID"),
            )
        } catch (e: Exception) {
            logger.error("Error creating subway station", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "지하철 역 ID로 조회", description = "지하철 역 ID로 지하철 역을 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "지하철 역 ID로 조회 성공",
                content = [Content(schema = Schema(implementation = SubwayStationResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "지하철 역을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "지하철 역을 찾을 수 없음",
                                value = """{"message":"SUBWAY_STATION_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getSubwayStationById(
        @PathVariable id: String,
    ): ResponseEntity<*> {
        try {
            return ResponseBuilder.response(
                HttpStatus.OK,
                service.getStationById(id).let {
                    SubwayStationResponse(
                        id = it.id,
                        name = it.name,
                        routeID = it.routeID,
                        order = it.order,
                        cumulativeTime = LocalDateTimeBuilder.convertDurationToString(it.cumulativeTime),
                    )
                },
            )
        } catch (_: SubwayStationNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SUBWAY_STATION_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error fetching subway station by ID", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "지하철 역 정보 수정", description = "지하철 역 정보를 수정합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "지하철 역 정보 수정 성공",
                content = [Content(schema = Schema(implementation = SubwayStationResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "잘못된 시간 형식",
                                value = """{"message": "INVALID_TIME_FORMAT"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "지하철 역을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "지하철 역을 찾을 수 없음",
                                value = """{"message":"SUBWAY_STATION_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun updateSubwayStation(
        @PathVariable id: String,
        @RequestBody payload: UpdateSubwayStationRequest,
    ): ResponseEntity<*> {
        try {
            return ResponseBuilder.response(
                HttpStatus.OK,
                service.updateStation(id, payload).let {
                    SubwayStationResponse(
                        id = it.id,
                        name = it.name,
                        routeID = it.routeID,
                        order = it.order,
                        cumulativeTime = LocalDateTimeBuilder.convertDurationToString(it.cumulativeTime),
                    )
                },
            )
        } catch (_: DurationNotValidException) {
            return ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("INVALID_TIME_FORMAT"),
            )
        } catch (_: SubwayStationNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SUBWAY_STATION_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error updating subway station", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "지하철 역 삭제", description = "지하철 역 정보를 삭제합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "지하철 역 삭제 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "지하철 역을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "지하철 역을 찾을 수 없음",
                                value = """{"message":"SUBWAY_STATION_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun deleteSubwayStation(
        @PathVariable id: String,
    ): ResponseEntity<*> {
        try {
            service.deleteStation(id)
            return ResponseBuilder.response(
                HttpStatus.NO_CONTENT,
                null,
            )
        } catch (_: SubwayStationNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SUBWAY_STATION_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error deleting subway station", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @GetMapping("/{id}/timetable")
    @Operation(summary = "지하철 역 시간표 조회", description = "지하철 역의 시간표를 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "지하철 역 시간표 조회 성공",
                content = [Content(schema = Schema(implementation = SubwayStationListResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "지하철 역을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "지하철 역을 찾을 수 없음",
                                value = """{"message":"SUBWAY_STATION_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getSubwayStationTimetable(
        @PathVariable id: String,
    ): ResponseEntity<*> {
        try {
            return ResponseBuilder.response(
                HttpStatus.OK,
                SubwayTimetableListResponse(
                    service.getTimetablesByStationID(id).map {
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
                ),
            )
        } catch (_: SubwayStationNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SUBWAY_STATION_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error fetching subway station timetable", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @PostMapping("/{id}/timetable")
    @Operation(summary = "지하철 역 시간표 추가", description = "지하철 역의 시간표를 추가합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "지하철 역 시간표 추가 성공",
                content = [Content(schema = Schema(implementation = SubwayTimetableResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "잘못된 시간 형식",
                                value = """{"message": "INVALID_TIME_FORMAT"}""",
                            ),
                            ExampleObject(
                                name = "존재하지 않는 시점 ID",
                                value = """{"message": "SUBWAY_START_STATION_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "존재하지 않는 종점 ID",
                                value = """{"message": "SUBWAY_TERMINAL_STATION_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "지하철 역을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "지하철 역을 찾을 수 없음",
                                value = """{"message":"SUBWAY_STATION_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "중복된 지하철 시간표",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "중복된 지하철 시간표",
                                value = """{"message":"DUPLICATE_SUBWAY_TIMETABLE"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun addSubwayStationTimetable(
        @PathVariable id: String,
        @RequestBody payload: SubwayTimetableRequest,
    ): ResponseEntity<*> {
        try {
            return ResponseBuilder.response(
                HttpStatus.CREATED,
                service.createTimetable(id, payload).let {
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
        } catch (_: SubwayStationNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SUBWAY_STATION_NOT_FOUND"),
            )
        } catch (_: LocalTimeNotValidException) {
            return ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("INVALID_TIME_FORMAT"),
            )
        } catch (_: SubwayStartStationNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("SUBWAY_START_STATION_NOT_FOUND"),
            )
        } catch (_: SubwayTerminalStationNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("SUBWAY_TERMINAL_STATION_NOT_FOUND"),
            )
        } catch (_: DuplicateSubwayTimetableException) {
            return ResponseBuilder.response(
                HttpStatus.CONFLICT,
                ResponseBuilder.Message("DUPLICATE_SUBWAY_TIMETABLE"),
            )
        } catch (e: Exception) {
            logger.error("Error adding subway station timetable", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @GetMapping("/{id}/timetable/{seq}")
    @Operation(summary = "지하철 역 시간표 조회", description = "지하철 역의 특정 시간표를 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "지하철 역 시간표 조회 성공",
                content = [Content(schema = Schema(implementation = SubwayTimetableResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "지하철 역 또는 시간표를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "지하철 역을 찾을 수 없음",
                                value = """{"message":"SUBWAY_STATION_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "지하철 시간표를 찾을 수 없음",
                                value = """{"message":"SUBWAY_TIMETABLE_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getSubwayStationTimetableBySeq(
        @PathVariable id: String,
        @PathVariable seq: Int,
    ): ResponseEntity<*> {
        try {
            return ResponseBuilder.response(
                HttpStatus.OK,
                service.getTimetableByStationIDAndSeq(id, seq).let {
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
        } catch (_: SubwayStationNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SUBWAY_STATION_NOT_FOUND"),
            )
        } catch (_: SubwayTimetableNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SUBWAY_TIMETABLE_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error fetching subway station timetable by seq", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @PutMapping("/{id}/timetable/{seq}")
    @Operation(summary = "지하철 역 시간표 수정", description = "지하철 역의 시간표를 수정합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "지하철 역 시간표 수정 성공",
                content = [Content(schema = Schema(implementation = SubwayTimetableResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "잘못된 시간 형식",
                                value = """{"message": "INVALID_TIME_FORMAT"}""",
                            ),
                            ExampleObject(
                                name = "존재하지 않는 시점 ID",
                                value = """{"message": "SUBWAY_START_STATION_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "존재하지 않는 종점 ID",
                                value = """{"message": "SUBWAY_TERMINAL_STATION_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "지하철 역 또는 시간표를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "지하철 역을 찾을 수 없음",
                                value = """{"message":"SUBWAY_STATION_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "지하철 시간표를 찾을 수 없음",
                                value = """{"message":"SUBWAY_TIMETABLE_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun updateSubwayStationTimetable(
        @PathVariable id: String,
        @PathVariable seq: Int,
        @RequestBody payload: SubwayTimetableRequest,
    ): ResponseEntity<*> {
        try {
            return ResponseBuilder.response(
                HttpStatus.OK,
                service.updateTimetable(id, seq, payload).let {
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
        } catch (_: SubwayStationNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SUBWAY_STATION_NOT_FOUND"),
            )
        } catch (_: SubwayStartStationNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("SUBWAY_START_STATION_NOT_FOUND"),
            )
        } catch (_: SubwayTerminalStationNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("SUBWAY_TERMINAL_STATION_NOT_FOUND"),
            )
        } catch (_: LocalTimeNotValidException) {
            return ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("INVALID_TIME_FORMAT"),
            )
        } catch (_: SubwayTimetableNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SUBWAY_TIMETABLE_NOT_FOUND"),
            )
        } catch (_: DuplicateSubwayTimetableException) {
            return ResponseBuilder.response(
                HttpStatus.CONFLICT,
                ResponseBuilder.Message("DUPLICATE_SUBWAY_TIMETABLE"),
            )
        } catch (e: Exception) {
            logger.error("Error updating subway station timetable", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @DeleteMapping("/{id}/timetable/{seq}")
    @Operation(summary = "지하철 역 시간표 삭제", description = "지하철 역의 시간표를 삭제합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "지하철 역 시간표 삭제 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "지하철 역 또는 시간표를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "지하철 역을 찾을 수 없음",
                                value = """{"message":"SUBWAY_STATION_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "지하철 시간표를 찾을 수 없음",
                                value = """{"message":"SUBWAY_TIMETABLE_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun deleteSubwayStationTimetable(
        @PathVariable id: String,
        @PathVariable seq: Int,
    ): ResponseEntity<*> {
        try {
            service.deleteTimetable(id, seq)
            return ResponseBuilder.response(
                HttpStatus.NO_CONTENT,
                null,
            )
        } catch (_: SubwayStationNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SUBWAY_STATION_NOT_FOUND"),
            )
        } catch (_: SubwayTimetableNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SUBWAY_TIMETABLE_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error deleting subway station timetable", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }
}
