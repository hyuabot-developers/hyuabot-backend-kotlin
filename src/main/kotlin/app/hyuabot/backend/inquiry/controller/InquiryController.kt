package app.hyuabot.backend.inquiry.controller

import app.hyuabot.backend.database.entity.InquiryMessage
import app.hyuabot.backend.database.entity.InquiryThread
import app.hyuabot.backend.inquiry.InquiryService
import app.hyuabot.backend.inquiry.domain.MessageListResponse
import app.hyuabot.backend.inquiry.domain.MessageResponse
import app.hyuabot.backend.inquiry.domain.OpenThreadRequest
import app.hyuabot.backend.inquiry.domain.SendMessageRequest
import app.hyuabot.backend.inquiry.domain.ThreadResponse
import app.hyuabot.backend.inquiry.exception.EmptyInquiryMessageException
import app.hyuabot.backend.inquiry.exception.InquiryThreadForbiddenException
import app.hyuabot.backend.inquiry.exception.InquiryThreadNotFoundException
import app.hyuabot.backend.utility.ResponseBuilder
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RequestMapping("/api/v1/inquiry")
@RestController
@Tag(name = "Inquiry", description = "문의 채팅(사용자) 관련 API")
class InquiryController {
    @Autowired private lateinit var service: InquiryService
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/threads")
    @Operation(summary = "문의 스레드 생성", description = "활성 스레드가 있으면 반환하고, 없으면 새로 생성합니다.")
    @ApiResponse(
        responseCode = "201",
        description = "문의 스레드 생성 성공",
        content = [Content(schema = Schema(implementation = ThreadResponse::class))],
    )
    fun openThread(
        @RequestHeader("X-Installation-Id") installationId: UUID,
        @RequestHeader(value = "X-App-Platform", required = false) platform: String?,
        @RequestHeader(value = "X-App-Version", required = false) appVersion: String?,
        @RequestBody request: OpenThreadRequest,
    ): ResponseEntity<*> =
        try {
            val thread =
                service.openOrGetActiveThread(
                    installationId = installationId,
                    platform = platform ?: "UNKNOWN",
                    appVersion = appVersion,
                    subject = request.subject,
                    contactEmail = request.contactEmail,
                    entryScreen = request.entryScreen,
                    entryScreenName = request.entryScreenName,
                )
            ResponseBuilder.response(HttpStatus.CREATED, toThreadResponse(thread))
        } catch (e: Exception) {
            logger.error("Failed to open inquiry thread", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @GetMapping("/threads/me")
    @Operation(summary = "내 활성 문의 스레드 조회", description = "활성 스레드가 있으면 반환하고, 없으면 204를 반환합니다.")
    @ApiResponse(
        responseCode = "200",
        description = "활성 문의 스레드 조회 성공",
        content = [Content(schema = Schema(implementation = ThreadResponse::class))],
    )
    fun getMyThread(
        @RequestHeader("X-Installation-Id") installationId: UUID,
    ): ResponseEntity<*> =
        try {
            service.getActiveThread(installationId)?.let {
                ResponseBuilder.response(HttpStatus.OK, toThreadResponse(it))
            } ?: ResponseBuilder.response(HttpStatus.NO_CONTENT, null)
        } catch (e: Exception) {
            logger.error("Failed to get active inquiry thread", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @GetMapping("/threads/{id}/messages")
    @Operation(summary = "문의 메시지 목록 조회", description = "스레드의 메시지를 조회합니다. after 이후의 메시지만 조회할 수 있습니다.")
    @ApiResponse(
        responseCode = "200",
        description = "문의 메시지 목록 조회 성공",
        content = [Content(schema = Schema(implementation = MessageListResponse::class))],
    )
    fun getMessages(
        @RequestHeader("X-Installation-Id") installationId: UUID,
        @PathVariable id: UUID,
        @RequestParam(value = "after", required = false) after: Long?,
    ): ResponseEntity<*> =
        try {
            val messages = service.getMessages(id, installationId, after)
            ResponseBuilder.response(
                HttpStatus.OK,
                MessageListResponse(messages.map { toMessageResponse(it) }),
            )
        } catch (_: InquiryThreadForbiddenException) {
            ResponseBuilder.response(HttpStatus.FORBIDDEN, ResponseBuilder.Message("FORBIDDEN"))
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
    @Operation(summary = "문의 메시지 전송", description = "스레드에 사용자 메시지를 전송합니다.")
    @ApiResponse(
        responseCode = "201",
        description = "문의 메시지 전송 성공",
        content = [Content(schema = Schema(implementation = MessageResponse::class))],
    )
    fun sendMessage(
        @RequestHeader("X-Installation-Id") installationId: UUID,
        @PathVariable id: UUID,
        @RequestBody request: SendMessageRequest,
    ): ResponseEntity<*> =
        try {
            val message = service.sendUserMessage(id, installationId, request.body)
            ResponseBuilder.response(HttpStatus.CREATED, toMessageResponse(message))
        } catch (_: InquiryThreadForbiddenException) {
            ResponseBuilder.response(HttpStatus.FORBIDDEN, ResponseBuilder.Message("FORBIDDEN"))
        } catch (_: InquiryThreadNotFoundException) {
            ResponseBuilder.response(HttpStatus.NOT_FOUND, ResponseBuilder.Message("THREAD_NOT_FOUND"))
        } catch (_: EmptyInquiryMessageException) {
            ResponseBuilder.response(HttpStatus.BAD_REQUEST, ResponseBuilder.Message("EMPTY_MESSAGE"))
        } catch (e: Exception) {
            logger.error("Failed to send inquiry message", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    @PostMapping("/threads/{id}/read")
    @Operation(summary = "문의 메시지 읽음 처리", description = "관리자가 보낸 메시지를 읽음 처리합니다.")
    @ApiResponse(responseCode = "204", description = "문의 메시지 읽음 처리 성공")
    fun markRead(
        @RequestHeader("X-Installation-Id") installationId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<*> =
        try {
            service.markReadByUser(id, installationId)
            ResponseBuilder.response(HttpStatus.NO_CONTENT, null)
        } catch (_: InquiryThreadForbiddenException) {
            ResponseBuilder.response(HttpStatus.FORBIDDEN, ResponseBuilder.Message("FORBIDDEN"))
        } catch (_: InquiryThreadNotFoundException) {
            ResponseBuilder.response(HttpStatus.NOT_FOUND, ResponseBuilder.Message("THREAD_NOT_FOUND"))
        } catch (e: Exception) {
            logger.error("Failed to mark inquiry messages read", e)
            ResponseBuilder.response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ResponseBuilder.Message("INTERNAL_SERVER_ERROR"),
            )
        }

    private fun toThreadResponse(thread: InquiryThread): ThreadResponse =
        ThreadResponse(
            id = thread.id.toString(),
            status = thread.status,
            subject = thread.subject,
            contactEmail = thread.contactEmail,
            entryScreen = thread.entryScreen,
            entryScreenName = thread.entryScreenName,
            lastMessageAt = thread.lastMessageAt?.toString(),
            createdAt = thread.createdAt.toString(),
        )

    private fun toMessageResponse(message: InquiryMessage): MessageResponse =
        MessageResponse(
            id = message.id!!,
            senderType = message.senderType,
            body = message.body,
            readAt = message.readAt?.toString(),
            createdAt = message.createdAt.toString(),
        )
}
