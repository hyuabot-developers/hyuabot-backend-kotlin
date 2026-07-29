package app.hyuabot.backend.inquiry.controller

import app.hyuabot.backend.database.entity.InquiryThread
import app.hyuabot.backend.inquiry.InquiryService
import app.hyuabot.backend.inquiry.domain.AdminThreadListResponse
import app.hyuabot.backend.inquiry.domain.AdminThreadResponse
import app.hyuabot.backend.inquiry.domain.MessageListResponse
import app.hyuabot.backend.inquiry.domain.MessageResponse
import app.hyuabot.backend.inquiry.domain.PatchThreadRequest
import app.hyuabot.backend.inquiry.domain.SendMessageRequest
import app.hyuabot.backend.inquiry.domain.toMessageResponse
import app.hyuabot.backend.inquiry.exception.EmptyInquiryMessageException
import app.hyuabot.backend.inquiry.exception.InquiryThreadNotFoundException
import app.hyuabot.backend.inquiry.exception.InvalidInquiryStatusException
import app.hyuabot.backend.inquiry.sse.InquirySseRegistry
import app.hyuabot.backend.utility.ResponseBuilder
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID

@RequestMapping("/api/v1/inquiry/admin")
@RestController
@Tag(name = "InquiryAdmin", description = "문의 채팅(관리자) 관련 API")
class InquiryAdminController {
    @Autowired private lateinit var service: InquiryService

    @Autowired private lateinit var registry: InquirySseRegistry
    private val logger = LoggerFactory.getLogger(javaClass)

    private fun currentAdminUserId(): String = SecurityContextHolder.getContext().authentication!!.name

    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    @Operation(summary = "문의 실시간 스트림(관리자)", description = "모든 문의 이벤트를 SSE로 수신합니다.")
    @ApiResponse(responseCode = "200", description = "SSE 연결 성공")
    fun stream(): SseEmitter = registry.registerForAdmin()

    @GetMapping("/threads")
    @Operation(summary = "문의 스레드 목록 조회", description = "assigned=true이면 내게 배정된 스레드만, 아니면 활성 스레드 전체를 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "문의 스레드 목록 조회 성공",
        content = [Content(schema = Schema(implementation = AdminThreadListResponse::class))],
    )
    fun listThreads(
        @RequestParam(value = "assigned", required = false, defaultValue = "false") assigned: Boolean,
    ): ResponseEntity<*> =
        try {
            val threads =
                if (assigned) {
                    service.adminListThreads(currentAdminUserId())
                } else {
                    service.adminListThreads(null)
                }
            ResponseBuilder.response(
                HttpStatus.OK,
                AdminThreadListResponse(threads.map { toAdminThreadResponse(it) }),
            )
        } catch (e: Exception) {
            logger.error("Failed to list inquiry threads", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @GetMapping("/threads/{id}")
    @Operation(summary = "문의 스레드 단건 조회", description = "문의 스레드를 단건 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "문의 스레드 단건 조회 성공",
        content = [Content(schema = Schema(implementation = AdminThreadResponse::class))],
    )
    fun getThread(
        @PathVariable id: UUID,
    ): ResponseEntity<*> =
        try {
            ResponseBuilder.response(HttpStatus.OK, toAdminThreadResponse(service.adminGetThread(id)))
        } catch (_: InquiryThreadNotFoundException) {
            ResponseBuilder.response(HttpStatus.NOT_FOUND, ResponseBuilder.Message("THREAD_NOT_FOUND"))
        } catch (e: Exception) {
            logger.error("Failed to get inquiry thread", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @GetMapping("/threads/{id}/messages")
    @Operation(summary = "문의 메시지 목록 조회", description = "스레드의 메시지를 조회합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "문의 메시지 목록 조회 성공",
        content = [Content(schema = Schema(implementation = MessageListResponse::class))],
    )
    fun getMessages(
        @PathVariable id: UUID,
    ): ResponseEntity<*> =
        try {
            service.adminGetThread(id)
            val messages = service.getMessagesForAdmin(id)
            ResponseBuilder.response(
                HttpStatus.OK,
                MessageListResponse(messages.map { it.toMessageResponse() }),
            )
        } catch (_: InquiryThreadNotFoundException) {
            ResponseBuilder.response(HttpStatus.NOT_FOUND, ResponseBuilder.Message("THREAD_NOT_FOUND"))
        } catch (e: Exception) {
            logger.error("Failed to get inquiry messages", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PostMapping("/threads/{id}/messages")
    @Operation(summary = "문의 답변 전송", description = "스레드에 관리자 답변을 전송합니다.")
    @ApiResponse(
        responseCode = "201",
        description = "문의 답변 전송 성공",
        content = [Content(schema = Schema(implementation = MessageResponse::class))],
    )
    fun reply(
        @PathVariable id: UUID,
        @RequestBody request: SendMessageRequest,
    ): ResponseEntity<*> =
        try {
            val message = service.adminReply(id, currentAdminUserId(), request.body)
            ResponseBuilder.response(HttpStatus.CREATED, message.toMessageResponse())
        } catch (_: InquiryThreadNotFoundException) {
            ResponseBuilder.response(HttpStatus.NOT_FOUND, ResponseBuilder.Message("THREAD_NOT_FOUND"))
        } catch (_: EmptyInquiryMessageException) {
            ResponseBuilder.response(HttpStatus.BAD_REQUEST, ResponseBuilder.Message("EMPTY_MESSAGE"))
        } catch (e: Exception) {
            logger.error("Failed to send inquiry reply", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PostMapping("/threads/{id}/read")
    @Operation(summary = "문의 메시지 읽음 처리", description = "사용자가 보낸 메시지를 읽음 처리합니다.")
    @ApiResponse(responseCode = "204", description = "문의 메시지 읽음 처리 성공")
    fun markRead(
        @PathVariable id: UUID,
    ): ResponseEntity<*> =
        try {
            service.adminMarkRead(id)
            ResponseBuilder.response(HttpStatus.NO_CONTENT, null)
        } catch (_: InquiryThreadNotFoundException) {
            ResponseBuilder.response(HttpStatus.NOT_FOUND, ResponseBuilder.Message("THREAD_NOT_FOUND"))
        } catch (e: Exception) {
            logger.error("Failed to mark inquiry messages read", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PatchMapping("/threads/{id}")
    @Operation(summary = "문의 스레드 수정", description = "스레드의 상태 또는 배정 관리자를 수정합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "문의 스레드 수정 성공",
        content = [Content(schema = Schema(implementation = AdminThreadResponse::class))],
    )
    fun updateThread(
        @PathVariable id: UUID,
        @RequestBody request: PatchThreadRequest,
    ): ResponseEntity<*> =
        try {
            val thread = service.adminUpdateThread(id, request.status, request.assignedAdminUserId)
            ResponseBuilder.response(HttpStatus.OK, toAdminThreadResponse(thread))
        } catch (_: InquiryThreadNotFoundException) {
            ResponseBuilder.response(HttpStatus.NOT_FOUND, ResponseBuilder.Message("THREAD_NOT_FOUND"))
        } catch (_: InvalidInquiryStatusException) {
            ResponseBuilder.response(HttpStatus.BAD_REQUEST, ResponseBuilder.Message("INVALID_STATUS"))
        } catch (e: Exception) {
            logger.error("Failed to update inquiry thread", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @DeleteMapping("/threads/{id}")
    @Operation(summary = "문의 스레드 종료", description = "문의 스레드를 종료(삭제)합니다.")
    @ApiResponse(responseCode = "204", description = "문의 스레드 종료 성공")
    fun closeThread(
        @PathVariable id: UUID,
    ): ResponseEntity<*> =
        try {
            service.adminCloseThread(id)
            ResponseBuilder.response(HttpStatus.NO_CONTENT, null)
        } catch (_: InquiryThreadNotFoundException) {
            ResponseBuilder.response(HttpStatus.NOT_FOUND, ResponseBuilder.Message("THREAD_NOT_FOUND"))
        } catch (e: Exception) {
            logger.error("Failed to close inquiry thread", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    private fun toAdminThreadResponse(thread: InquiryThread): AdminThreadResponse =
        AdminThreadResponse(
            id = thread.id.toString(),
            installationId = thread.installationId.toString(),
            platform = thread.platform,
            status = thread.status,
            subject = thread.subject,
            contactEmail = thread.contactEmail,
            assignedAdminUserId = thread.assignedAdminUserId,
            entryScreen = thread.entryScreen,
            entryScreenName = thread.entryScreenName,
            lastMessageAt = thread.lastMessageAt?.toString(),
            createdAt = thread.createdAt.toString(),
        )
}
