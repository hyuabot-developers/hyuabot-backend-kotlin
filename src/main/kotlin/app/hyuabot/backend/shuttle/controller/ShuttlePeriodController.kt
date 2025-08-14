package app.hyuabot.backend.shuttle.controller

import app.hyuabot.backend.database.exception.LocalDateTimeNotValidException
import app.hyuabot.backend.shuttle.domain.ShuttlePeriodListResponse
import app.hyuabot.backend.shuttle.domain.ShuttlePeriodRequest
import app.hyuabot.backend.shuttle.domain.ShuttlePeriodResponse
import app.hyuabot.backend.shuttle.exception.ShuttlePeriodNotFoundException
import app.hyuabot.backend.shuttle.service.ShuttlePeriodService
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

@RequestMapping("/api/v1/shuttle/period")
@RestController
@Tag(name = "Shuttle", description = "셔틀버스 관련 API")
class ShuttlePeriodController {
    @Autowired private lateinit var shuttlePeriodService: ShuttlePeriodService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("")
    @Operation(summary = "셔틀버스 운행 주기 조회", description = "셔틀버스 운행 주기를 조회합니다.")
    fun getShuttlePeriod(): ShuttlePeriodListResponse =
        ShuttlePeriodListResponse(
            result =
                shuttlePeriodService.getShuttlePeriodList().map {
                    ShuttlePeriodResponse(
                        seq = it.seq!!,
                        type = it.type,
                        start =
                            LocalDateTimeBuilder.convertLocalDateTimeToString(
                                LocalDateTimeBuilder.convertAsServiceTimezone(it.start),
                            ),
                        end =
                            LocalDateTimeBuilder.convertLocalDateTimeToString(
                                LocalDateTimeBuilder.convertAsServiceTimezone(it.end),
                            ),
                    )
                },
        )

    @PostMapping("")
    @Operation(
        summary = "셔틀버스 운행 주기 추가",
        description = "셔틀버스 운행 주기를 추가합니다.",
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "셔틀버스 운행 주기 정보",
                content = [Content(schema = Schema(implementation = ShuttlePeriodRequest::class))],
            ),
    )
    @ApiResponse(
        responseCode = "201",
        description = "셔틀버스 운행 주기 추가 성공",
        content = [Content(schema = Schema(implementation = ShuttlePeriodResponse::class))],
    )
    fun createShuttlePeriod(
        @RequestBody payload: ShuttlePeriodRequest,
    ): ResponseEntity<*> {
        try {
            val period = shuttlePeriodService.createShuttlePeriod(payload)
            return ResponseBuilder.response(
                HttpStatus.CREATED,
                ShuttlePeriodResponse(
                    seq = period.seq!!,
                    type = period.type,
                    start =
                        LocalDateTimeBuilder.convertLocalDateTimeToString(
                            LocalDateTimeBuilder.convertAsServiceTimezone(period.start),
                        ),
                    end =
                        LocalDateTimeBuilder.convertLocalDateTimeToString(
                            LocalDateTimeBuilder.convertAsServiceTimezone(period.end),
                        ),
                ),
            )
        } catch (_: LocalDateTimeNotValidException) {
            return ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message(
                    message = "INVALID_DATE_TIME_FORMAT",
                ),
            )
        } catch (e: Exception) {
            logger.error("Error creating shuttle period: ${e.message}", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message(
                    message = "INTERNAL_SERVER_ERROR",
                ),
            )
        }
    }

    @GetMapping("/{seq}")
    @Operation(
        summary = "셔틀버스 운행 주기 조회",
        description = "셔틀버스 운행 주기를 조회합니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "seq",
                description = "셔틀버스 운행 주기 ID",
                required = true,
                example = "1",
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "셔틀버스 운행 주기 조회 성공",
                content = [Content(schema = Schema(implementation = ShuttlePeriodResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "셔틀버스 운행 주기를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "셔틀버스 운행 주기 조회 실패",
                                summary = "셔틀버스 운행 주기를 찾을 수 없음",
                                value = """{"message": "SHUTTLE_PERIOD_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getShuttlePeriod(
        @PathVariable seq: Int,
    ): ResponseEntity<*> =
        try {
            val period = shuttlePeriodService.getShuttlePeriod(seq)
            ResponseBuilder.response(
                HttpStatus.OK,
                ShuttlePeriodResponse(
                    seq = period.seq!!,
                    type = period.type,
                    start =
                        LocalDateTimeBuilder.convertLocalDateTimeToString(
                            LocalDateTimeBuilder.convertAsServiceTimezone(period.start),
                        ),
                    end =
                        LocalDateTimeBuilder.convertLocalDateTimeToString(
                            LocalDateTimeBuilder.convertAsServiceTimezone(period.end),
                        ),
                ),
            )
        } catch (_: ShuttlePeriodNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message(
                    message = "SHUTTLE_PERIOD_NOT_FOUND",
                ),
            )
        } catch (e: Exception) {
            logger.error("Error retrieving shuttle period: ${e.message}", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message(
                    message = "INTERNAL_SERVER_ERROR",
                ),
            )
        }

    @PutMapping("/{seq}")
    @Operation(
        summary = "셔틀버스 운행 주기 수정",
        description = "셔틀버스 운행 주기를 수정합니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "seq",
                description = "셔틀버스 운행 주기 ID",
                required = true,
                example = "1",
            ),
        ],
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "셔틀버스 운행 주기 정보",
                content = [Content(schema = Schema(implementation = ShuttlePeriodRequest::class))],
            ),
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "셔틀버스 운행 주기 수정 성공",
                content = [Content(schema = Schema(implementation = ShuttlePeriodResponse::class))],
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
                description = "셔틀버스 운행 주기를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "셔틀버스 운행 주기 수정 실패",
                                summary = "셔틀버스 운행 주기를 찾을 수 없음",
                                value = """{"message": "SHUTTLE_PERIOD_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun updateShuttlePeriod(
        @PathVariable seq: Int,
        @RequestBody payload: ShuttlePeriodRequest,
    ): ResponseEntity<*> {
        try {
            val period = shuttlePeriodService.updateShuttlePeriod(seq, payload)
            return ResponseBuilder.response(
                HttpStatus.OK,
                ShuttlePeriodResponse(
                    seq = period.seq!!,
                    type = period.type,
                    start =
                        LocalDateTimeBuilder.convertLocalDateTimeToString(
                            LocalDateTimeBuilder.convertAsServiceTimezone(period.start),
                        ),
                    end =
                        LocalDateTimeBuilder.convertLocalDateTimeToString(
                            LocalDateTimeBuilder.convertAsServiceTimezone(period.end),
                        ),
                ),
            )
        } catch (_: ShuttlePeriodNotFoundException) {
            return ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message(
                    message = "SHUTTLE_PERIOD_NOT_FOUND",
                ),
            )
        } catch (_: LocalDateTimeNotValidException) {
            return ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message(
                    message = "INVALID_DATE_TIME_FORMAT",
                ),
            )
        } catch (e: Exception) {
            logger.error("Error updating shuttle period: ${e.message}", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message(
                    message = "INTERNAL_SERVER_ERROR",
                ),
            )
        }
    }

    @DeleteMapping("/{seq}")
    @Operation(
        summary = "셔틀버스 운행 주기 삭제",
        description = "셔틀버스 운행 주기를 삭제합니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "seq",
                description = "셔틀버스 운행 주기 ID",
                required = true,
                example = "1",
            ),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "셔틀버스 운행 주기 삭제 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "셔틀버스 운행 주기를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "셔틀버스 운행 주기 삭제 실패",
                                summary = "셔틀버스 운행 주기를 찾을 수 없음",
                                value = """{"message": "SHUTTLE_PERIOD_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun deleteShuttlePeriod(
        @PathVariable seq: Int,
    ): ResponseEntity<*> =
        try {
            shuttlePeriodService.deleteShuttlePeriod(seq)
            ResponseBuilder.response(
                HttpStatus.NO_CONTENT,
                null,
            )
        } catch (_: ShuttlePeriodNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message(
                    message = "SHUTTLE_PERIOD_NOT_FOUND",
                ),
            )
        } catch (e: Exception) {
            logger.error("Error deleting shuttle period: ${e.message}", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message(
                    message = "INTERNAL_SERVER_ERROR",
                ),
            )
        }
}
