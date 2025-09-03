package app.hyuabot.backend.notice.controller

import app.hyuabot.backend.database.exception.LocalDateTimeNotValidException
import app.hyuabot.backend.notice.NoticeService
import app.hyuabot.backend.notice.domain.NoticeListResponse
import app.hyuabot.backend.notice.domain.NoticeRequest
import app.hyuabot.backend.notice.domain.NoticeResponse
import app.hyuabot.backend.notice.exception.NoticeCategoryNotFoundException
import app.hyuabot.backend.notice.exception.NoticeNotFoundException
import app.hyuabot.backend.security.JWTUser
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
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/notice/notices")
@RestController
@Tag(name = "Notice", description = "공지사항 API")
class NoticeController {
    @Autowired private lateinit var service: NoticeService
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("")
    @Operation(summary = "공지사항 목록 조회")
    @ApiResponse(
        responseCode = "200",
        description = "공지사항 목록 조회 성공",
        content = [Content(schema = Schema(implementation = NoticeListResponse::class))],
    )
    fun getNotices(): NoticeListResponse =
        NoticeListResponse(
            result =
                service.getAllNotices().map {
                    NoticeResponse(
                        seq = it.id!!,
                        title = it.title,
                        url = it.url,
                        expiredAt =
                            LocalDateTimeBuilder.convertLocalDateTimeToString(
                                LocalDateTimeBuilder.convertAsServiceTimezone(it.expiredAt),
                            ),
                        userID = it.userID,
                        language = it.language,
                    )
                },
        )

    @PostMapping("")
    @Operation(summary = "공지사항 등록", description = "공지사항을 등록합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "공지사항 등록 성공",
                content = [Content(schema = Schema(implementation = NoticeResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "공지사항 등록 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "존재하지 않는 카테고리",
                                value = """{"message":"CATEGORY_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "날짜 형식 오류",
                                value = """{"message":"INVALID_DATE_TIME_FORMAT"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun createNotice(
        @RequestBody payload: NoticeRequest,
    ): ResponseEntity<*> =
        try {
            val userID = (SecurityContextHolder.getContext().authentication.principal as JWTUser).username
            service.createNotice(userID, payload).let {
                ResponseBuilder.response(
                    status = HttpStatus.CREATED,
                    body =
                        NoticeResponse(
                            seq = it.id!!,
                            title = it.title,
                            url = it.url,
                            expiredAt =
                                LocalDateTimeBuilder.convertLocalDateTimeToString(
                                    LocalDateTimeBuilder.convertAsServiceTimezone(it.expiredAt),
                                ),
                            userID = it.userID,
                            language = it.language,
                        ),
                )
            }
        } catch (_: LocalDateTimeNotValidException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("INVALID_DATE_TIME_FORMAT"),
            )
        } catch (_: NoticeCategoryNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("CATEGORY_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("공지사항 등록 실패: ${e.message}")
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @GetMapping("/{seq}")
    @Operation(summary = "공지사항 단건 조회", description = "공지사항을 seq로 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "공지사항 단건 조회 성공",
                content = [Content(schema = Schema(implementation = NoticeResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "공지사항 단건 조회 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "존재하지 않는 공지사항",
                                value = """{"message":"NOTICE_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun getNotice(
        @PathVariable seq: Int,
    ): ResponseEntity<*> =
        try {
            service.getNoticesById(seq).let {
                ResponseBuilder.response(
                    status = HttpStatus.OK,
                    body =
                        NoticeResponse(
                            seq = it.id!!,
                            title = it.title,
                            url = it.url,
                            expiredAt =
                                LocalDateTimeBuilder.convertLocalDateTimeToString(
                                    LocalDateTimeBuilder.convertAsServiceTimezone(it.expiredAt),
                                ),
                            userID = it.userID,
                            language = it.language,
                        ),
                )
            }
        } catch (_: NoticeNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("NOTICE_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("공지사항 단건 조회 실패: ${e.message}")
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PutMapping("/{seq}")
    @Operation(summary = "공지사항 수정", description = "공지사항을 수정합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "공지사항 수정 성공",
                content = [Content(schema = Schema(implementation = NoticeResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "공지사항 수정 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "존재하지 않는 카테고리",
                                value = """{"message":"CATEGORY_NOT_FOUND"}""",
                            ),
                            ExampleObject(
                                name = "날짜 형식 오류",
                                value = """{"message":"INVALID_DATE_TIME_FORMAT"}""",
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "공지사항 수정 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ResponseBuilder.Message::class),
                        examples = [
                            ExampleObject(
                                name = "존재하지 않는 공지사항",
                                value = """{"message":"NOTICE_NOT_FOUND"}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun updateNotice(
        @PathVariable seq: Int,
        @RequestBody payload: NoticeRequest,
    ): ResponseEntity<*> =
        try {
            service.updateNotice(seq, payload).let {
                ResponseBuilder.response(
                    status = HttpStatus.OK,
                    body =
                        NoticeResponse(
                            seq = it.id!!,
                            title = it.title,
                            url = it.url,
                            expiredAt =
                                LocalDateTimeBuilder.convertLocalDateTimeToString(
                                    LocalDateTimeBuilder.convertAsServiceTimezone(it.expiredAt),
                                ),
                            userID = it.userID,
                            language = it.language,
                        ),
                )
            }
        } catch (_: LocalDateTimeNotValidException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("INVALID_DATE_TIME_FORMAT"),
            )
        } catch (_: NoticeCategoryNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.BAD_REQUEST,
                ResponseBuilder.Message("CATEGORY_NOT_FOUND"),
            )
        } catch (_: NoticeNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("NOTICE_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("공지사항 수정 실패: ${e.message}")
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @DeleteMapping("/{seq}")
    @Operation(summary = "공지사항 삭제", description = "공지사항을 삭제합니다.")
    @ApiResponses(
        ApiResponse(
            responseCode = "204",
            description = "공지사항 삭제 성공",
        ),
        ApiResponse(
            responseCode = "404",
            description = "공지사항 삭제 실패",
            content = [
                Content(
                    schema = Schema(implementation = ResponseBuilder.Message::class),
                    examples = [
                        ExampleObject(
                            name = "존재하지 않는 공지사항",
                            value = """{"message":"NOTICE_NOT_FOUND"}""",
                        ),
                    ],
                ),
            ],
        ),
    )
    fun deleteNotice(
        @PathVariable seq: Int,
    ): ResponseEntity<*> =
        try {
            service.deleteNoticeById(seq)
            ResponseBuilder.response(
                HttpStatus.NO_CONTENT,
                null,
            )
        } catch (_: NoticeNotFoundException) {
            ResponseBuilder.response(
                HttpStatus.NOT_FOUND,
                ResponseBuilder.Message("NOTICE_NOT_FOUND"),
            )
        } catch (e: Exception) {
            logger.error("공지사항 삭제 실패: ${e.message}")
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }
}
