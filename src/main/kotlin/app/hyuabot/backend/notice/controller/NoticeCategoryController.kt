package app.hyuabot.backend.notice.controller

import app.hyuabot.backend.notice.NoticeService
import app.hyuabot.backend.notice.domain.NoticeCategoryListResponse
import app.hyuabot.backend.notice.domain.NoticeCategoryRequest
import app.hyuabot.backend.notice.domain.NoticeCategoryResponse
import app.hyuabot.backend.notice.domain.NoticeListResponse
import app.hyuabot.backend.notice.domain.NoticeResponse
import app.hyuabot.backend.notice.exception.DuplicateCategoryException
import app.hyuabot.backend.notice.exception.NoticeCategoryNotFoundException
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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/notice/category")
@RestController
@Tag(name = "Notice", description = "공지사항 API")
class NoticeCategoryController {
    @Autowired private lateinit var service: NoticeService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("")
    @Operation(summary = "공지사항 카테고리 조회", description = "공지사항 카테고리를 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "공지사항 카테고리 조회 성공",
        content = [Content(schema = Schema(implementation = NoticeCategoryListResponse::class))],
    )
    fun getNoticeCategory(): NoticeCategoryListResponse =
        NoticeCategoryListResponse(
            result =
                service.getNoticeCategories().map {
                    NoticeCategoryResponse(
                        seq = it.id!!,
                        name = it.name,
                    )
                },
        )

    @PostMapping("")
    @Operation(
        summary = "공지사항 카테고리 생성",
        description = "공지사항 카테고리를 생성합니다.",
        requestBody =
            io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "공지사항 카테고리 생성 요청",
                required = true,
                content = [Content(schema = Schema(implementation = NoticeCategoryRequest::class))],
            ),
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "201",
            description = "공지사항 카테고리 생성 성공",
            content = [Content(schema = Schema(implementation = NoticeCategoryResponse::class))],
        ),
        ApiResponse(
            responseCode = "409",
            description = "공지사항 카테고리 생성 실패 - 중복된 이름",
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
    fun createNoticeCategory(
        @RequestBody payload: NoticeCategoryRequest,
    ): ResponseEntity<*> {
        try {
            service.createNoticeCategory(payload).let {
                return ResponseBuilder.response(
                    HttpStatus.CREATED,
                    NoticeCategoryResponse(
                        seq = it.id!!,
                        name = it.name,
                    ),
                )
            }
        } catch (_: DuplicateCategoryException) {
            return ResponseBuilder.response(
                HttpStatus.CONFLICT,
                ResponseBuilder.Message("DUPLICATE_CATEGORY_NAME"),
            )
        } catch (e: Exception) {
            logger.error("Error creating notice category", e)
            return ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
    }

    @GetMapping("/{seq}")
    @Operation(
        summary = "공지사항 카테고리 항목 조회",
        description = "공지사항 카테고리 항목을 조회합니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "seq",
                description = "공지사항 카테고리 시퀀스",
                example = "1",
                required = true,
            ),
        ],
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "공지사항 카테고리 항목 조회 성공",
            content = [Content(schema = Schema(implementation = NoticeResponse::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "공지사항 카테고리 항목 조회 실패 - 카테고리 없음",
            content = [
                Content(
                    schema = Schema(implementation = ResponseBuilder.Message::class),
                    examples = [
                        ExampleObject(
                            name = "카테고리 없음",
                            value = """{"message": "CATEGORY_NOT_FOUND"}""",
                        ),
                    ],
                ),
            ],
        ),
    )
    fun getNoticeCategoryById(
        @PathVariable seq: Int,
    ): ResponseEntity<*> =
        try {
            service.getNoticeCategoryById(seq).let {
                ResponseBuilder.response(
                    HttpStatus.OK,
                    NoticeCategoryResponse(
                        seq = it.id!!,
                        name = it.name,
                    ),
                )
            }
        } catch (_: NoticeCategoryNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("CATEGORY_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error retrieving category $seq", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @DeleteMapping("/{seq}")
    @Operation(
        summary = "공지사항 카테고리 삭제",
        description = "공지사항 카테고리를 삭제합니다. 해당 카테고리에 속한 공지사항도 모두 삭제됩니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "seq",
                description = "공지사항 카테고리 시퀀스",
                example = "1",
                required = true,
            ),
        ],
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "204",
            description = "공지사항 카테고리 삭제 성공",
        ),
        ApiResponse(
            responseCode = "404",
            description = "공지사항 카테고리 삭제 실패 - 카테고리 없음",
            content = [
                Content(
                    schema = Schema(implementation = ResponseBuilder.Message::class),
                    examples = [
                        ExampleObject(
                            name = "카테고리 없음",
                            value = """{"message": "CATEGORY_NOT_FOUND"}""",
                        ),
                    ],
                ),
            ],
        ),
    )
    fun deleteNoticeCategory(
        @PathVariable seq: Int,
    ): ResponseEntity<*> =
        try {
            service.deleteNoticeCategoryById(seq)
            ResponseBuilder.response(
                HttpStatus.NO_CONTENT,
                null,
            )
        } catch (_: NoticeCategoryNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("CATEGORY_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error deleting category $seq", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @GetMapping("/{seq}/notices")
    @Operation(
        summary = "공지사항 카테고리별 공지사항 조회",
        description = "특정 공지사항 카테고리에 속한 공지사항들을 조회합니다.",
        parameters = [
            io.swagger.v3.oas.annotations.Parameter(
                name = "seq",
                description = "공지사항 카테고리 시퀀스",
                example = "1",
                required = true,
            ),
        ],
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "공지사항 카테고리별 공지사항 조회 성공",
            content = [Content(schema = Schema(implementation = NoticeListResponse::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "공지사항 카테고리별 공지사항 조회 실패 - 카테고리 없음",
            content = [
                Content(
                    schema = Schema(implementation = ResponseBuilder.Message::class),
                    examples = [
                        ExampleObject(
                            name = "카테고리 없음",
                            value = """{"message": "CATEGORY_NOT_FOUND"}""",
                        ),
                    ],
                ),
            ],
        ),
    )
    fun getNoticesByCategoryId(
        @PathVariable seq: Int,
    ): ResponseEntity<*> =
        try {
            service.getNoticesByCategoryId(seq).let { notices ->
                ResponseBuilder.response(
                    HttpStatus.OK,
                    NoticeListResponse(
                        result =
                            notices.map {
                                NoticeResponse(
                                    seq = it.id!!,
                                    title = it.title,
                                    url = it.url,
                                    expiredAt =
                                        LocalDateTimeBuilder.convertLocalDateTimeToString(
                                            LocalDateTimeBuilder.convertAsServiceTimezone(it.expiredAt),
                                        ),
                                    userID = it.userID,
                                    categoryID = it.categoryID,
                                    language = it.language,
                                )
                            },
                    ),
                )
            }
        } catch (_: NoticeCategoryNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("CATEGORY_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("Error retrieving notices for category $seq", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
}
