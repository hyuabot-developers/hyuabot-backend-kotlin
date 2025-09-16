package app.hyuabot.backend.shuttle.controller

import app.hyuabot.backend.database.exception.LocalDateNotValidException
import app.hyuabot.backend.shuttle.domain.ShuttleHolidayListResponse
import app.hyuabot.backend.shuttle.domain.ShuttleHolidayRequest
import app.hyuabot.backend.shuttle.domain.ShuttleHolidayResponse
import app.hyuabot.backend.shuttle.exception.DuplicateShuttleHolidayException
import app.hyuabot.backend.shuttle.exception.ShuttleHolidayNotFoundException
import app.hyuabot.backend.shuttle.service.ShuttleHolidayService
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

@RequestMapping("/api/v1/shuttle/holiday")
@RestController
@Tag(name = "Shuttle", description = "셔틀버스 관련 API")
class ShuttleHolidayController {
    @Autowired
    private lateinit var service: ShuttleHolidayService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("")
    @Operation(summary = "셔틀버스 휴일 정보 조회", description = "셔틀버스 휴일 정보를 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "셔틀버스 휴일 정보 조회 성공",
        content = [Content(schema = Schema(implementation = ShuttleHolidayListResponse::class))],
    )
    fun getShuttleHoliday(): ShuttleHolidayListResponse =
        ShuttleHolidayListResponse(
            service.getShuttleHolidayList().map {
                ShuttleHolidayResponse(
                    seq = it.seq!!,
                    date = LocalDateTimeBuilder.convertLocalDateToString(it.date),
                    calendarType = it.calendarType,
                    type = it.type,
                )
            },
        )

    @PostMapping("")
    @Operation(summary = "셔틀버스 휴일 정보 생성", description = "셔틀버스 휴일 정보를 생성합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "셔틀버스 휴일 정보 생성 성공",
                content = [Content(schema = Schema(implementation = ShuttleHolidayResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "잘못된 날짜 형식",
                                summary = "date 필드에 잘못된 날짜 형식이 들어온 경우",
                                value = """{"message": "LOCAL_DATE_NOT_VALID"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "중복된 휴일 정보",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "중복된 휴일 정보",
                                summary = "이미 존재하는 날짜와 음력/양력 구분으로 휴일 정보를 생성하려는 경우",
                                value = """{"message": "DUPLICATE_SHUTTLE_HOLIDAY"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun createShuttleHoliday(
        @RequestBody payload: ShuttleHolidayRequest,
    ): ResponseEntity<*> =
        try {
            service.createShuttleHoliday(payload).let {
                ResponseBuilder.response(
                    HttpStatus.CREATED,
                    ShuttleHolidayResponse(
                        seq = it.seq!!,
                        date = LocalDateTimeBuilder.convertLocalDateToString(it.date),
                        calendarType = it.calendarType,
                        type = it.type,
                    ),
                )
            }
        } catch (_: LocalDateNotValidException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("LOCAL_DATE_NOT_VALID"),
            )
        } catch (_: DuplicateShuttleHolidayException) {
            ResponseBuilder.response(
                HttpStatus.CONFLICT,
                ResponseBuilder.Message("DUPLICATE_SHUTTLE_HOLIDAY"),
            )
        } catch (e: Exception) {
            logger.error("Error occurred while creating shuttle holiday", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @GetMapping("/{seq}")
    @Operation(summary = "셔틀버스 휴일 정보 조회", description = "셔틀버스 휴일 정보를 seq로 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "셔틀버스 휴일 정보 조회 성공",
                content = [Content(schema = Schema(implementation = ShuttleHolidayResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "휴일 정보 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "휴일 정보 없음",
                                summary = "해당 seq의 휴일 정보가 존재하지 않는 경우",
                                value = """{"message": "SHUTTLE_HOLIDAY_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getShuttleHolidayBySeq(
        @PathVariable seq: Int,
    ): ResponseEntity<*> =
        try {
            service.getShuttleHolidayById(seq).let {
                ResponseBuilder.response(
                    HttpStatus.OK,
                    ShuttleHolidayResponse(
                        seq = it.seq!!,
                        date = LocalDateTimeBuilder.convertLocalDateToString(it.date),
                        calendarType = it.calendarType,
                        type = it.type,
                    ),
                )
            }
        } catch (_: ShuttleHolidayNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_HOLIDAY_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error occurred while retrieving shuttle holiday by seq", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PutMapping("/{seq}")
    @Operation(summary = "셔틀버스 휴일 정보 수정", description = "셔틀버스 휴일 정보를 수정합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "셔틀버스 휴일 정보 수정 성공",
                content = [Content(schema = Schema(implementation = ShuttleHolidayResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "잘못된 날짜 형식",
                                summary = "date 필드에 잘못된 날짜 형식이 들어온 경우",
                                value = """{"message": "LOCAL_DATE_NOT_VALID"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "휴일 정보 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "휴일 정보 없음",
                                summary = "해당 seq의 휴일 정보가 존재하지 않는 경우",
                                value = """{"message": "SHUTTLE_HOLIDAY_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "중복된 휴일 정보",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "중복된 휴일 정보",
                                summary = "이미 존재하는 날짜와 음력/양력 구분으로 휴일 정보를 생성하려는 경우",
                                value = """{"message": "DUPLICATE_SHUTTLE_HOLIDAY"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun updateShuttleHoliday(
        @PathVariable seq: Int,
        @RequestBody payload: ShuttleHolidayRequest,
    ): ResponseEntity<*> =
        try {
            service.updateShuttleHoliday(seq, payload).let {
                ResponseBuilder.response(
                    HttpStatus.OK,
                    ShuttleHolidayResponse(
                        seq = it.seq!!,
                        date = LocalDateTimeBuilder.convertLocalDateToString(it.date),
                        calendarType = it.calendarType,
                        type = it.type,
                    ),
                )
            }
        } catch (_: LocalDateNotValidException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("LOCAL_DATE_NOT_VALID"),
            )
        } catch (_: ShuttleHolidayNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_HOLIDAY_NOT_FOUND"),
            )
        } catch (_: DuplicateShuttleHolidayException) {
            ResponseBuilder.response(
                HttpStatus.CONFLICT,
                ResponseBuilder.Message("DUPLICATE_SHUTTLE_HOLIDAY"),
            )
        } catch (e: Exception) {
            logger.error("Error occurred while updating shuttle holiday", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @DeleteMapping("/{seq}")
    @Operation(summary = "셔틀버스 휴일 정보 삭제", description = "셔틀버스 휴일 정보를 삭제합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "셔틀버스 휴일 정보 삭제 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "휴일 정보 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "휴일 정보 없음",
                                summary = "해당 seq의 휴일 정보가 존재하지 않는 경우",
                                value = """{"message": "SHUTTLE_HOLIDAY_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun deleteShuttleHoliday(
        @PathVariable seq: Int,
    ): ResponseEntity<*> =
        try {
            service.deleteShuttleHoliday(seq)
            ResponseBuilder.response(
                HttpStatus.NO_CONTENT,
                null,
            )
        } catch (_: ShuttleHolidayNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("SHUTTLE_HOLIDAY_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error occurred while deleting shuttle holiday", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
}
