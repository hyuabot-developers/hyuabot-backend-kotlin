package app.hyuabot.backend.calendar.controller

import app.hyuabot.backend.calendar.CalendarService
import app.hyuabot.backend.calendar.domain.CalendarEventListResponse
import app.hyuabot.backend.calendar.domain.CalendarEventRequest
import app.hyuabot.backend.calendar.domain.CalendarEventResponse
import app.hyuabot.backend.calendar.exception.CalendarEventNotFoundException
import app.hyuabot.backend.database.exception.LocalDateTimeNotValidException
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
import java.time.format.DateTimeFormatter

@RequestMapping("/api/v1/calendar/events")
@RestController
@Tag(name = "Calendar", description = "학사일정 관련 API")
class CalendarEventController {
    @Autowired private lateinit var service: CalendarService
    private val logger = LoggerFactory.getLogger(javaClass)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    @GetMapping("")
    @Operation(summary = "학사일정 전체 조회", description = "학사일정을 전체 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "학사일정 전체 조회 성공",
        content = [Content(schema = Schema(implementation = CalendarEventListResponse::class))],
    )
    fun getAllEvents(): CalendarEventListResponse =
        CalendarEventListResponse(
            service.getAllEvents().map {
                CalendarEventResponse(
                    seq = it.id!!,
                    title = it.title,
                    description = it.description,
                    start = it.start.format(dateTimeFormatter),
                    end = it.end.format(dateTimeFormatter),
                    categoryID = it.categoryID,
                )
            },
        )

    @PostMapping("")
    @Operation(summary = "학사일정 등록", description = "학사일정을 등록합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "학사일정 등록 성공",
                content = [Content(schema = Schema(implementation = CalendarEventResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "학사일정 등록 실패",
                content = [Content(schema = Schema(implementation = ResponseBuilder.Message::class))],
            ),
        ],
    )
    fun createEvent(
        @RequestBody request: CalendarEventRequest,
    ): ResponseEntity<*> =
        try {
            val event = service.createEvent(request)
            ResponseBuilder.response(
                HttpStatus.CREATED,
                CalendarEventResponse(
                    seq = event.id!!,
                    title = event.title,
                    description = event.description,
                    start = event.start.format(dateTimeFormatter),
                    end = event.end.format(dateTimeFormatter),
                    categoryID = event.categoryID,
                ),
            )
        } catch (_: LocalDateTimeNotValidException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("LOCAL_DATE_TIME_NOT_VALID"),
            )
        } catch (e: Exception) {
            logger.error("Failed to create event", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @GetMapping("/{seq}")
    @Operation(
        summary = "학사일정 단건 조회",
        description = "학사일정을 단건 조회합니다.",
        parameters = [
            io.swagger.v3.oas.annotations
                .Parameter(name = "seq", description = "학사일정 ID", example = "1"),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "학사일정 단건 조회 성공",
                content = [Content(schema = Schema(implementation = CalendarEventResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "학사일정 단건 조회 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "존재하지 않는 학사일정",
                                value = """{"message":"EVENT_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getEvent(
        @PathVariable seq: Int,
    ): ResponseEntity<*> =
        try {
            val event = service.getEventById(seq)
            ResponseBuilder.response(
                HttpStatus.OK,
                CalendarEventResponse(
                    seq = event.id!!,
                    title = event.title,
                    description = event.description,
                    start = event.start.format(dateTimeFormatter),
                    end = event.end.format(dateTimeFormatter),
                    categoryID = event.categoryID,
                ),
            )
        } catch (_: CalendarEventNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("EVENT_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Failed to get event", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PutMapping("/{seq}")
    @Operation(
        summary = "학사일정 수정",
        description = "학사일정을 수정합니다.",
        parameters = [
            io.swagger.v3.oas.annotations
                .Parameter(name = "seq", description = "학사일정 ID", example = "1"),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "학사일정 수정 성공",
                content = [Content(schema = Schema(implementation = CalendarEventResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "학사일정 수정 실패 - 잘못된 요청",
                content = [Content(schema = Schema(implementation = ResponseBuilder.Message::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "학사일정 수정 실패 - 학사일정 없음",
                content = [Content(schema = Schema(implementation = ResponseBuilder.Message::class))],
            ),
        ],
    )
    fun updateEvent(
        @PathVariable seq: Int,
        @RequestBody request: CalendarEventRequest,
    ): ResponseEntity<*> =
        try {
            val event = service.updateEvent(seq, request)
            ResponseBuilder.response(
                HttpStatus.OK,
                CalendarEventResponse(
                    seq = event.id!!,
                    title = event.title,
                    description = event.description,
                    start = event.start.format(dateTimeFormatter),
                    end = event.end.format(dateTimeFormatter),
                    categoryID = event.categoryID,
                ),
            )
        } catch (_: CalendarEventNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("EVENT_NOT_FOUND"),
            )
        } catch (_: LocalDateTimeNotValidException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("LOCAL_DATE_TIME_NOT_VALID"),
            )
        } catch (e: Exception) {
            logger.error("Failed to update event", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @DeleteMapping("/{seq}")
    @Operation(
        summary = "학사일정 삭제",
        description = "학사일정을 삭제합니다.",
        parameters = [
            io.swagger.v3.oas.annotations
                .Parameter(name = "seq", description = "학사일정 ID", example = "1"),
        ],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "학사일정 삭제 성공",
                content = [Content(schema = Schema(implementation = ResponseBuilder.Message::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "학사일정 삭제 실패 - 학사일정 없음",
                content = [Content(schema = Schema(implementation = ResponseBuilder.Message::class))],
            ),
        ],
    )
    fun deleteEvent(
        @PathVariable seq: Int,
    ): ResponseEntity<*> =
        try {
            service.deleteEventById(seq)
            ResponseBuilder.response(
                HttpStatus.NO_CONTENT,
                null,
            )
        } catch (_: CalendarEventNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("EVENT_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Failed to delete event", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
}
