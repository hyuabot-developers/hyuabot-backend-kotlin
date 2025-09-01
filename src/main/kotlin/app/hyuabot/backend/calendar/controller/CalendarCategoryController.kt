package app.hyuabot.backend.calendar.controller

import app.hyuabot.backend.calendar.CalendarService
import app.hyuabot.backend.calendar.domain.CalendarCategoryListResponse
import app.hyuabot.backend.calendar.domain.CalendarCategoryRequest
import app.hyuabot.backend.calendar.domain.CalendarCategoryResponse
import app.hyuabot.backend.calendar.domain.CalendarEventListResponse
import app.hyuabot.backend.calendar.domain.CalendarEventResponse
import app.hyuabot.backend.calendar.exception.CalendarCategoryNotFoundException
import app.hyuabot.backend.calendar.exception.DuplicateCategoryException
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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/calendar/category")
@RestController
@Tag(name = "Calendar", description = "학사일정 관련 API")
class CalendarCategoryController {
    @Autowired private lateinit var service: CalendarService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("")
    @Operation(summary = "학사일정 카테고리 조회", description = "학사일정 카테고리를 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "학사일정 카테고리 조회 성공",
        content = [Content(schema = Schema(implementation = CalendarCategoryListResponse::class))],
    )
    fun getCalendarCategory(): CalendarCategoryListResponse =
        CalendarCategoryListResponse(
            service.getCalendarCategories().map {
                CalendarCategoryResponse(
                    seq = it.id!!,
                    name = it.name,
                )
            },
        )

    @PostMapping("")
    @Operation(
        summary = "학사일정 카테고리 추가",
        description = "학사일정 카테고리를 추가합니다.",
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = [Content(schema = Schema(implementation = CalendarCategoryRequest::class))],
                description = "추가할 학사일정 카테고리 정보",
            ),
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "201",
            description = "학사일정 카테고리 추가 성공",
            content = [Content(schema = Schema(implementation = CalendarCategoryResponse::class))],
        ),
        ApiResponse(
            responseCode = "409",
            description = "학사일정 카테고리 추가 실패 - 중복된 이름",
            content = [
                Content(
                    schema = Schema(implementation = ResponseBuilder.Message::class),
                    examples = [
                        ExampleObject(
                            name = "중복된 이름",
                            value = """{"message": "DUPLICATE_CATEGORY_NAME"}""",
                        ),
                    ],
                ),
            ],
        ),
    )
    fun createCalendarCategory(
        @RequestBody request: CalendarCategoryRequest,
    ): ResponseEntity<*> {
        try {
            val category = service.createCalendarCategory(request)
            return ResponseBuilder.response(
                status = HttpStatus.CREATED,
                body =
                    CalendarCategoryResponse(
                        seq = category.id!!,
                        name = category.name,
                    ),
            )
        } catch (_: DuplicateCategoryException) {
            return ResponseBuilder.response(
                status = HttpStatus.CONFLICT,
                body = ResponseBuilder.Message("DUPLICATE_CATEGORY_NAME"),
            )
        } catch (e: Exception) {
            logger.error("Unexpected error occurred while creating calendar category", e)
            return ResponseBuilder.response(
                status = HttpStatus.INTERNAL_SERVER_ERROR,
                body = ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @GetMapping("/{seq}")
    @Operation(
        summary = "학사일정 카테고리 조회",
        description = "학사일정 카테고리를 조회합니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "seq",
                description = "학사일정 카테고리 ID",
                example = "1",
                required = true,
            ),
        ],
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "학사일정 카테고리 조회 성공",
            content = [Content(schema = Schema(implementation = CalendarCategoryResponse::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "학사일정 카테고리 조회 실패 - 존재하지 않는 ID",
            content = [
                Content(
                    schema = Schema(implementation = ResponseBuilder.Message::class),
                    examples = [
                        ExampleObject(
                            name = "존재하지 않는 ID",
                            value = """{"message": "CATEGORY_NOT_FOUND"}""",
                        ),
                    ],
                ),
            ],
        ),
    )
    fun getCalendarCategoryById(
        @PathVariable seq: Int,
    ): ResponseEntity<*> {
        try {
            return service.getCalendarCategoryById(seq).let {
                ResponseBuilder.response(
                    status = HttpStatus.OK,
                    body =
                        CalendarCategoryResponse(
                            seq = it.id!!,
                            name = it.name,
                        ),
                )
            }
        } catch (_: CalendarCategoryNotFoundException) {
            return ResponseBuilder.response(
                status = HttpStatus.NOT_FOUND,
                body = ResponseBuilder.Message("CATEGORY_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Unexpected error occurred while retrieving calendar category", e)
            return ResponseBuilder.response(
                status = HttpStatus.INTERNAL_SERVER_ERROR,
                body = ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @DeleteMapping("/{seq}")
    @Operation(
        summary = "학사일정 카테고리 삭제",
        description = "학사일정 카테고리를 삭제합니다. 해당 카테고리에 속한 일정도 모두 삭제됩니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "seq",
                description = "학사일정 카테고리 ID",
                example = "1",
                required = true,
            ),
        ],
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "204",
            description = "학사일정 카테고리 삭제 성공",
        ),
        ApiResponse(
            responseCode = "404",
            description = "학사일정 카테고리 삭제 실패 - 존재하지 않는 ID",
            content = [
                Content(
                    schema = Schema(implementation = ResponseBuilder.Message::class),
                    examples = [
                        ExampleObject(
                            name = "존재하지 않는 ID",
                            value = """{"message": "CATEGORY_NOT_FOUND"}""",
                        ),
                    ],
                ),
            ],
        ),
    )
    fun deleteCalendarCategory(
        @PathVariable seq: Int,
    ): ResponseEntity<*> {
        try {
            service.deleteCalendarCategoryById(seq)
            return ResponseBuilder.response(
                status = HttpStatus.NO_CONTENT,
                body = null,
            )
        } catch (_: CalendarCategoryNotFoundException) {
            return ResponseBuilder.response(
                status = HttpStatus.NOT_FOUND,
                body = ResponseBuilder.Message("CATEGORY_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Unexpected error occurred while deleting calendar category", e)
            return ResponseBuilder.response(
                status = HttpStatus.INTERNAL_SERVER_ERROR,
                body = ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @GetMapping("/{seq}/events")
    @Operation(
        summary = "학사일정 카테고리에 속한 일정 조회",
        description = "학사일정 카테고리에 속한 일정을 조회합니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "seq",
                description = "학사일정 카테고리 ID",
                example = "1",
                required = true,
            ),
        ],
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "학사일정 카테고리에 속한 일정 조회 성공",
            content = [Content(schema = Schema(implementation = CalendarEventListResponse::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "학사일정 카테고리에 속한 일정 조회 실패 - 존재하지 않는 ID",
            content = [
                Content(
                    schema = Schema(implementation = ResponseBuilder.Message::class),
                    examples = [
                        ExampleObject(
                            name = "존재하지 않는 ID",
                            value = """{"message": "CATEGORY_NOT_FOUND"}""",
                        ),
                    ],
                ),
            ],
        ),
    )
    fun getEventsByCategoryId(
        @PathVariable seq: Int,
    ): ResponseEntity<*> {
        try {
            return ResponseBuilder.response(
                status = HttpStatus.OK,
                body =
                    CalendarEventListResponse(
                        service.getEventByCategoryId(seq).map {
                            CalendarEventResponse(
                                seq = it.id!!,
                                title = it.title,
                                description = it.description,
                                start = it.start.toString(),
                                end = it.end.toString(),
                                categoryID = it.categoryID,
                            )
                        },
                    ),
            )
        } catch (_: CalendarCategoryNotFoundException) {
            return ResponseBuilder.response(
                status = HttpStatus.NOT_FOUND,
                body = ResponseBuilder.Message("CATEGORY_NOT_FOUND"),
            )
        }
    }
}
