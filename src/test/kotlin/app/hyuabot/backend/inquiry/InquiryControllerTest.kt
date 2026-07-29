package app.hyuabot.backend.inquiry

import app.hyuabot.backend.database.entity.InquiryMessage
import app.hyuabot.backend.database.entity.InquiryThread
import app.hyuabot.backend.inquiry.domain.OpenThreadRequest
import app.hyuabot.backend.inquiry.domain.SendMessageRequest
import app.hyuabot.backend.inquiry.exception.EmptyInquiryMessageException
import app.hyuabot.backend.inquiry.exception.InquiryThreadForbiddenException
import app.hyuabot.backend.inquiry.exception.InquiryThreadNotFoundException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.time.ZonedDateTime
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InquiryControllerTest {
    @MockitoBean
    private lateinit var service: InquiryService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val installationId: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val threadId: UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")

    private fun thread() =
        InquiryThread(
            id = threadId,
            installationId = installationId,
            platform = "iOS",
            status = "OPEN",
            createdAt = ZonedDateTime.now(),
            updatedAt = ZonedDateTime.now(),
        )

    private fun message() =
        InquiryMessage(
            id = 1L,
            threadId = threadId,
            senderType = "ADMIN",
            body = "답변",
            createdAt = ZonedDateTime.now(),
        )

    private fun installationHeader() = installationId.toString()

    private fun threadWithLastMessage() =
        InquiryThread(
            id = threadId,
            installationId = installationId,
            platform = "iOS",
            status = "OPEN",
            lastMessageAt = ZonedDateTime.now(),
            createdAt = ZonedDateTime.now(),
            updatedAt = ZonedDateTime.now(),
        )

    private fun readMessage() =
        InquiryMessage(
            id = 2L,
            threadId = threadId,
            senderType = "ADMIN",
            body = "답변",
            readAt = ZonedDateTime.now(),
            createdAt = ZonedDateTime.now(),
        )

    @Test
    @DisplayName("문의 스레드 생성 - 플랫폼 헤더/마지막 메시지 시각 포함")
    fun testOpenThreadWithPlatformHeader() {
        doReturn(threadWithLastMessage()).whenever(service).openOrGetActiveThread(
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
        )
        mockMvc
            .perform(
                post("/api/v1/inquiry/threads")
                    .header("X-Installation-Id", installationHeader())
                    .header("X-App-Platform", "iOS")
                    .header("X-App-Version", "1.0.0")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(OpenThreadRequest(subject = "제목"))),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.lastMessageAt").isNotEmpty)
    }

    @Test
    @DisplayName("문의 메시지 목록 조회 - 읽은 메시지 포함")
    fun testGetMessagesWithReadMessage() {
        doReturn(listOf(readMessage())).whenever(service).getMessages(anyOrNull(), anyOrNull(), anyOrNull())
        mockMvc
            .perform(get("/api/v1/inquiry/threads/$threadId/messages").header("X-Installation-Id", installationHeader()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result[0].readAt").isNotEmpty)
    }

    @Test
    @DisplayName("문의 스레드 생성 - 성공")
    fun testOpenThread() {
        doReturn(thread()).whenever(service).openOrGetActiveThread(
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
        )
        mockMvc
            .perform(
                post("/api/v1/inquiry/threads")
                    .header("X-Installation-Id", installationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(OpenThreadRequest(subject = "제목"))),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(threadId.toString()))
            .andExpect(jsonPath("$.status").value("OPEN"))
    }

    @Test
    @DisplayName("문의 스레드 생성 - 기타 예외")
    fun testOpenThreadError() {
        doThrow(RuntimeException("error")).whenever(service).openOrGetActiveThread(
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
        )
        mockMvc
            .perform(
                post("/api/v1/inquiry/threads")
                    .header("X-Installation-Id", installationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(OpenThreadRequest())),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("내 활성 스레드 조회 - 200")
    fun testGetMyThread() {
        doReturn(thread()).whenever(service).getActiveThread(any())
        mockMvc
            .perform(get("/api/v1/inquiry/threads/me").header("X-Installation-Id", installationHeader()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(threadId.toString()))
    }

    @Test
    @DisplayName("내 활성 스레드 조회 - 204")
    fun testGetMyThreadNoContent() {
        doReturn(null).whenever(service).getActiveThread(any())
        mockMvc
            .perform(get("/api/v1/inquiry/threads/me").header("X-Installation-Id", installationHeader()))
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("내 활성 스레드 조회 - 기타 예외")
    fun testGetMyThreadError() {
        doThrow(RuntimeException("error")).whenever(service).getActiveThread(any())
        mockMvc
            .perform(get("/api/v1/inquiry/threads/me").header("X-Installation-Id", installationHeader()))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("문의 메시지 목록 조회 - 200")
    fun testGetMessages() {
        doReturn(listOf(message())).whenever(service).getMessages(anyOrNull(), anyOrNull(), anyOrNull())
        mockMvc
            .perform(get("/api/v1/inquiry/threads/$threadId/messages").header("X-Installation-Id", installationHeader()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.length()").value(1))
            .andExpect(jsonPath("$.result[0].id").value(1))
            .andExpect(jsonPath("$.result[0].senderType").value("ADMIN"))
            .andExpect(jsonPath("$.result[0].body").value("답변"))
    }

    @Test
    @DisplayName("문의 메시지 목록 조회 - 403")
    fun testGetMessagesForbidden() {
        doThrow(InquiryThreadForbiddenException()).whenever(service).getMessages(anyOrNull(), anyOrNull(), anyOrNull())
        mockMvc
            .perform(get("/api/v1/inquiry/threads/$threadId/messages").header("X-Installation-Id", installationHeader()))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("FORBIDDEN"))
    }

    @Test
    @DisplayName("문의 메시지 목록 조회 - 404")
    fun testGetMessagesNotFound() {
        doThrow(InquiryThreadNotFoundException()).whenever(service).getMessages(anyOrNull(), anyOrNull(), anyOrNull())
        mockMvc
            .perform(get("/api/v1/inquiry/threads/$threadId/messages").header("X-Installation-Id", installationHeader()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("THREAD_NOT_FOUND"))
    }

    @Test
    @DisplayName("문의 메시지 목록 조회 - 기타 예외")
    fun testGetMessagesError() {
        doThrow(RuntimeException("error")).whenever(service).getMessages(anyOrNull(), anyOrNull(), anyOrNull())
        mockMvc
            .perform(get("/api/v1/inquiry/threads/$threadId/messages").header("X-Installation-Id", installationHeader()))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("문의 메시지 전송 - 201")
    fun testSendMessage() {
        doReturn(message()).whenever(service).sendUserMessage(anyOrNull(), anyOrNull(), anyOrNull())
        mockMvc
            .perform(
                post("/api/v1/inquiry/threads/$threadId/messages")
                    .header("X-Installation-Id", installationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(SendMessageRequest(body = "안녕하세요"))),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.body").value("답변"))
    }

    @Test
    @DisplayName("문의 메시지 전송 - 403")
    fun testSendMessageForbidden() {
        doThrow(InquiryThreadForbiddenException()).whenever(service).sendUserMessage(anyOrNull(), anyOrNull(), anyOrNull())
        mockMvc
            .perform(
                post("/api/v1/inquiry/threads/$threadId/messages")
                    .header("X-Installation-Id", installationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(SendMessageRequest(body = "hi"))),
            ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("FORBIDDEN"))
    }

    @Test
    @DisplayName("문의 메시지 전송 - 404")
    fun testSendMessageNotFound() {
        doThrow(InquiryThreadNotFoundException()).whenever(service).sendUserMessage(anyOrNull(), anyOrNull(), anyOrNull())
        mockMvc
            .perform(
                post("/api/v1/inquiry/threads/$threadId/messages")
                    .header("X-Installation-Id", installationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(SendMessageRequest(body = "hi"))),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("THREAD_NOT_FOUND"))
    }

    @Test
    @DisplayName("문의 메시지 전송 - 400 빈 메시지")
    fun testSendMessageEmpty() {
        doThrow(EmptyInquiryMessageException()).whenever(service).sendUserMessage(anyOrNull(), anyOrNull(), anyOrNull())
        mockMvc
            .perform(
                post("/api/v1/inquiry/threads/$threadId/messages")
                    .header("X-Installation-Id", installationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(SendMessageRequest(body = " "))),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("EMPTY_MESSAGE"))
    }

    @Test
    @DisplayName("문의 메시지 전송 - 기타 예외")
    fun testSendMessageError() {
        doThrow(RuntimeException("error")).whenever(service).sendUserMessage(anyOrNull(), anyOrNull(), anyOrNull())
        mockMvc
            .perform(
                post("/api/v1/inquiry/threads/$threadId/messages")
                    .header("X-Installation-Id", installationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(SendMessageRequest(body = "hi"))),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("문의 메시지 읽음 처리 - 204")
    fun testMarkRead() {
        mockMvc
            .perform(post("/api/v1/inquiry/threads/$threadId/read").header("X-Installation-Id", installationHeader()))
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("문의 메시지 읽음 처리 - 403")
    fun testMarkReadForbidden() {
        doThrow(InquiryThreadForbiddenException()).whenever(service).markReadByUser(anyOrNull(), anyOrNull())
        mockMvc
            .perform(post("/api/v1/inquiry/threads/$threadId/read").header("X-Installation-Id", installationHeader()))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("FORBIDDEN"))
    }

    @Test
    @DisplayName("문의 메시지 읽음 처리 - 404")
    fun testMarkReadNotFound() {
        doThrow(InquiryThreadNotFoundException()).whenever(service).markReadByUser(anyOrNull(), anyOrNull())
        mockMvc
            .perform(post("/api/v1/inquiry/threads/$threadId/read").header("X-Installation-Id", installationHeader()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("THREAD_NOT_FOUND"))
    }

    @Test
    @DisplayName("문의 실시간 스트림 - 200 SSE 연결")
    fun testStream() {
        mockMvc
            .perform(get("/api/v1/inquiry/stream").header("X-Installation-Id", installationHeader()))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
    }

    @Test
    @DisplayName("문의 메시지 읽음 처리 - 기타 예외")
    fun testMarkReadError() {
        doThrow(RuntimeException("error")).whenever(service).markReadByUser(anyOrNull(), anyOrNull())
        mockMvc
            .perform(post("/api/v1/inquiry/threads/$threadId/read").header("X-Installation-Id", installationHeader()))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }
}
