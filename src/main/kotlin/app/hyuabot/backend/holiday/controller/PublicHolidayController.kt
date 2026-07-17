package app.hyuabot.backend.holiday.controller

import app.hyuabot.backend.database.exception.LocalDateNotValidException
import app.hyuabot.backend.holiday.domain.PublicHolidayListResponse
import app.hyuabot.backend.holiday.domain.PublicHolidayRequest
import app.hyuabot.backend.holiday.domain.PublicHolidayResponse
import app.hyuabot.backend.holiday.exception.DuplicatePublicHolidayException
import app.hyuabot.backend.holiday.exception.PublicHolidayNotFoundException
import app.hyuabot.backend.holiday.service.PublicHolidayService
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

@RequestMapping("/api/v1/holiday")
@RestController
@Tag(name = "Holiday", description = "공휴일 관련 API")
class PublicHolidayController {
    @Autowired
    private lateinit var service: PublicHolidayService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("")
    @Operation(summary = "공휴일 정보 조회", description = "공휴일 정보를 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "공휴일 정보 조회 성공",
        content = [Content(schema = Schema(implementation = PublicHolidayListResponse::class))],
    )
    fun getPublicHoliday(): PublicHolidayListResponse =
        PublicHolidayListResponse(
            service.getPublicHolidayList().map {
                PublicHolidayResponse(
                    seq = it.seq!!,
                    date = LocalDateTimeBuilder.convertLocalDateToString(it.date),
                    name = it.name,
                    calendarType = it.calendarType,
                )
            },
        )

    @PostMapping("")
    @Operation(summary = "공휴일 정보 생성", description = "공휴일 정보를 생성합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "공휴일 정보 생성 성공",
                content = [Content(schema = Schema(implementation = PublicHolidayResponse::class))],
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
                description = "중복된 공휴일 정보",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "중복된 공휴일 정보",
                                summary = "이미 존재하는 날짜와 음력/양력 구분으로 공휴일 정보를 생성하려는 경우",
                                value = """{"message": "DUPLICATE_PUBLIC_HOLIDAY"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun createPublicHoliday(
        @RequestBody payload: PublicHolidayRequest,
    ): ResponseEntity<*> =
        try {
            service.createPublicHoliday(payload).let {
                ResponseBuilder.response(
                    HttpStatus.CREATED,
                    PublicHolidayResponse(
                        seq = it.seq!!,
                        date = LocalDateTimeBuilder.convertLocalDateToString(it.date),
                        name = it.name,
                        calendarType = it.calendarType,
                    ),
                )
            }
        } catch (_: LocalDateNotValidException) {
            ResponseBuilder.response(HttpStatus.BAD_REQUEST, ResponseBuilder.Message("LOCAL_DATE_NOT_VALID"))
        } catch (_: IllegalArgumentException) {
            ResponseBuilder.response(HttpStatus.BAD_REQUEST, ResponseBuilder.Message("INVALID_HOLIDAY_TYPE"))
        } catch (_: DuplicatePublicHolidayException) {
            ResponseBuilder.response(HttpStatus.CONFLICT, ResponseBuilder.Message("DUPLICATE_PUBLIC_HOLIDAY"))
        } catch (e: Exception) {
            logger.error("Error occurred while creating public holiday", e)
            ResponseBuilder.response(HttpStatus.INTERNAL_SERVER_ERROR, ResponseBuilder.Message("INTERNAL_SERVER_ERROR"))
        }

    @GetMapping("/{seq}")
    @Operation(summary = "공휴일 정보 단건 조회", description = "공휴일 정보를 seq로 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "공휴일 정보 조회 성공",
                content = [Content(schema = Schema(implementation = PublicHolidayResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "공휴일 정보 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "공휴일 정보 없음",
                                value = """{"message": "PUBLIC_HOLIDAY_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getPublicHolidayBySeq(
        @PathVariable seq: Int,
    ): ResponseEntity<*> =
        try {
            service.getPublicHolidayById(seq).let {
                ResponseBuilder.response(
                    HttpStatus.OK,
                    PublicHolidayResponse(
                        seq = it.seq!!,
                        date = LocalDateTimeBuilder.convertLocalDateToString(it.date),
                        name = it.name,
                        calendarType = it.calendarType,
                    ),
                )
            }
        } catch (_: PublicHolidayNotFoundException) {
            ResponseBuilder.response(HttpStatus.NOT_FOUND, ResponseBuilder.Message("PUBLIC_HOLIDAY_NOT_FOUND"))
        } catch (e: Exception) {
            logger.error("Error occurred while retrieving public holiday by seq", e)
            ResponseBuilder.response(HttpStatus.INTERNAL_SERVER_ERROR, ResponseBuilder.Message("INTERNAL_SERVER_ERROR"))
        }

    @PutMapping("/{seq}")
    @Operation(summary = "공휴일 정보 수정", description = "공휴일 정보를 수정합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "공휴일 정보 수정 성공",
                content = [Content(schema = Schema(implementation = PublicHolidayResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [Content(schema = Schema(implementation = ResponseBuilder.Message::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "공휴일 정보 없음",
                content = [Content(schema = Schema(implementation = ResponseBuilder.Message::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "중복된 공휴일 정보",
                content = [Content(schema = Schema(implementation = ResponseBuilder.Message::class))],
            ),
        ],
    )
    fun updatePublicHoliday(
        @PathVariable seq: Int,
        @RequestBody payload: PublicHolidayRequest,
    ): ResponseEntity<*> =
        try {
            service.updatePublicHoliday(seq, payload).let {
                ResponseBuilder.response(
                    HttpStatus.OK,
                    PublicHolidayResponse(
                        seq = it.seq!!,
                        date = LocalDateTimeBuilder.convertLocalDateToString(it.date),
                        name = it.name,
                        calendarType = it.calendarType,
                    ),
                )
            }
        } catch (_: LocalDateNotValidException) {
            ResponseBuilder.response(HttpStatus.BAD_REQUEST, ResponseBuilder.Message("LOCAL_DATE_NOT_VALID"))
        } catch (_: IllegalArgumentException) {
            ResponseBuilder.response(HttpStatus.BAD_REQUEST, ResponseBuilder.Message("INVALID_HOLIDAY_TYPE"))
        } catch (_: PublicHolidayNotFoundException) {
            ResponseBuilder.response(HttpStatus.NOT_FOUND, ResponseBuilder.Message("PUBLIC_HOLIDAY_NOT_FOUND"))
        } catch (_: DuplicatePublicHolidayException) {
            ResponseBuilder.response(HttpStatus.CONFLICT, ResponseBuilder.Message("DUPLICATE_PUBLIC_HOLIDAY"))
        } catch (e: Exception) {
            logger.error("Error occurred while updating public holiday", e)
            ResponseBuilder.response(HttpStatus.INTERNAL_SERVER_ERROR, ResponseBuilder.Message("INTERNAL_SERVER_ERROR"))
        }

    @DeleteMapping("/{seq}")
    @Operation(summary = "공휴일 정보 삭제", description = "공휴일 정보를 삭제합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "공휴일 정보 삭제 성공"),
            ApiResponse(
                responseCode = "404",
                description = "공휴일 정보 없음",
                content = [Content(schema = Schema(implementation = ResponseBuilder.Message::class))],
            ),
        ],
    )
    fun deletePublicHoliday(
        @PathVariable seq: Int,
    ): ResponseEntity<*> =
        try {
            service.deletePublicHoliday(seq)
            ResponseBuilder.response(HttpStatus.NO_CONTENT, null)
        } catch (_: PublicHolidayNotFoundException) {
            ResponseBuilder.response(HttpStatus.NOT_FOUND, ResponseBuilder.Message("PUBLIC_HOLIDAY_NOT_FOUND"))
        } catch (e: Exception) {
            logger.error("Error occurred while deleting public holiday", e)
            ResponseBuilder.response(HttpStatus.INTERNAL_SERVER_ERROR, ResponseBuilder.Message("INTERNAL_SERVER_ERROR"))
        }
}
