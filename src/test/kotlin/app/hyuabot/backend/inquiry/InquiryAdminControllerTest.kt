package app.hyuabot.backend.inquiry

import app.hyuabot.backend.database.entity.InquiryMessage
import app.hyuabot.backend.database.entity.InquiryThread
import app.hyuabot.backend.inquiry.domain.PatchThreadRequest
import app.hyuabot.backend.inquiry.domain.SendMessageRequest
import app.hyuabot.backend.inquiry.exception.EmptyInquiryMessageException
import app.hyuabot.backend.inquiry.exception.InquiryThreadNotFoundException
import app.hyuabot.backend.inquiry.exception.InvalidInquiryStatusException
import app.hyuabot.backend.security.WithCustomMockUser
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
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
class InquiryAdminControllerTest {
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
            senderType = "USER",
            body = "문의",
            createdAt = ZonedDateTime.now(),
        )

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
            senderType = "USER",
            body = "문의",
            readAt = ZonedDateTime.now(),
            createdAt = ZonedDateTime.now(),
        )

    @Test
    @DisplayName("스레드 단건 조회 - 마지막 메시지 시각 포함")
    @WithCustomMockUser(username = "adminUser")
    fun testGetThreadWithLastMessage() {
        doReturn(threadWithLastMessage()).whenever(service).adminGetThread(threadId)
        mockMvc
            .perform(get("/api/v1/inquiry/admin/threads/$threadId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.lastMessageAt").isNotEmpty)
    }

    @Test
    @DisplayName("메시지 목록 조회 - 읽은 메시지 포함")
    @WithCustomMockUser(username = "adminUser")
    fun testGetMessagesWithReadMessage() {
        doReturn(thread()).whenever(service).adminGetThread(threadId)
        doReturn(listOf(readMessage())).whenever(service).getMessagesForAdmin(threadId)
        mockMvc
            .perform(get("/api/v1/inquiry/admin/threads/$threadId/messages"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result[0].readAt").isNotEmpty)
    }

    @Test
    @DisplayName("스레드 목록 조회 - 배정된 것만")
    @WithCustomMockUser(username = "adminUser")
    fun testListThreadsAssigned() {
        doReturn(listOf(thread())).whenever(service).adminListThreads("adminUser")
        mockMvc
            .perform(get("/api/v1/inquiry/admin/threads?assigned=true"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.length()").value(1))
            .andExpect(jsonPath("$.result[0].id").value(threadId.toString()))
        verify(service).adminListThreads("adminUser")
    }

    @Test
    @DisplayName("스레드 목록 조회 - 전체")
    @WithCustomMockUser(username = "adminUser")
    fun testListThreadsAll() {
        doReturn(listOf(thread())).whenever(service).adminListThreads(null)
        mockMvc
            .perform(get("/api/v1/inquiry/admin/threads"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.length()").value(1))
        verify(service).adminListThreads(null)
    }

    @Test
    @DisplayName("스레드 목록 조회 - 기타 예외")
    @WithCustomMockUser(username = "adminUser")
    fun testListThreadsError() {
        doThrow(RuntimeException("error")).whenever(service).adminListThreads(anyOrNull())
        mockMvc
            .perform(get("/api/v1/inquiry/admin/threads"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("스레드 단건 조회 - 200")
    @WithCustomMockUser(username = "adminUser")
    fun testGetThread() {
        doReturn(thread()).whenever(service).adminGetThread(any())
        mockMvc
            .perform(get("/api/v1/inquiry/admin/threads/$threadId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(threadId.toString()))
            .andExpect(jsonPath("$.installationId").value(installationId.toString()))
    }

    @Test
    @DisplayName("스레드 단건 조회 - 404")
    @WithCustomMockUser(username = "adminUser")
    fun testGetThreadNotFound() {
        doThrow(InquiryThreadNotFoundException()).whenever(service).adminGetThread(any())
        mockMvc
            .perform(get("/api/v1/inquiry/admin/threads/$threadId"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("THREAD_NOT_FOUND"))
    }

    @Test
    @DisplayName("스레드 단건 조회 - 기타 예외")
    @WithCustomMockUser(username = "adminUser")
    fun testGetThreadError() {
        doThrow(RuntimeException("error")).whenever(service).adminGetThread(any())
        mockMvc
            .perform(get("/api/v1/inquiry/admin/threads/$threadId"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("메시지 목록 조회 - 200")
    @WithCustomMockUser(username = "adminUser")
    fun testGetMessages() {
        doReturn(thread()).whenever(service).adminGetThread(any())
        doReturn(listOf(message())).whenever(service).getMessagesForAdmin(any())
        mockMvc
            .perform(get("/api/v1/inquiry/admin/threads/$threadId/messages"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result.length()").value(1))
            .andExpect(jsonPath("$.result[0].senderType").value("USER"))
    }

    @Test
    @DisplayName("메시지 목록 조회 - 404")
    @WithCustomMockUser(username = "adminUser")
    fun testGetMessagesNotFound() {
        doThrow(InquiryThreadNotFoundException()).whenever(service).adminGetThread(any())
        mockMvc
            .perform(get("/api/v1/inquiry/admin/threads/$threadId/messages"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("THREAD_NOT_FOUND"))
    }

    @Test
    @DisplayName("메시지 목록 조회 - 기타 예외")
    @WithCustomMockUser(username = "adminUser")
    fun testGetMessagesError() {
        doThrow(RuntimeException("error")).whenever(service).adminGetThread(any())
        mockMvc
            .perform(get("/api/v1/inquiry/admin/threads/$threadId/messages"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("답변 전송 - 201")
    @WithCustomMockUser(username = "adminUser")
    fun testReply() {
        doReturn(message()).whenever(service).adminReply(anyOrNull(), anyOrNull(), anyOrNull())
        mockMvc
            .perform(
                post("/api/v1/inquiry/admin/threads/$threadId/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(SendMessageRequest(body = "답변드립니다"))),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))
    }

    @Test
    @DisplayName("답변 전송 - 404")
    @WithCustomMockUser(username = "adminUser")
    fun testReplyNotFound() {
        doThrow(InquiryThreadNotFoundException()).whenever(service).adminReply(anyOrNull(), anyOrNull(), anyOrNull())
        mockMvc
            .perform(
                post("/api/v1/inquiry/admin/threads/$threadId/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(SendMessageRequest(body = "hi"))),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("THREAD_NOT_FOUND"))
    }

    @Test
    @DisplayName("답변 전송 - 400 빈 메시지")
    @WithCustomMockUser(username = "adminUser")
    fun testReplyEmpty() {
        doThrow(EmptyInquiryMessageException()).whenever(service).adminReply(anyOrNull(), anyOrNull(), anyOrNull())
        mockMvc
            .perform(
                post("/api/v1/inquiry/admin/threads/$threadId/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(SendMessageRequest(body = " "))),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("EMPTY_MESSAGE"))
    }

    @Test
    @DisplayName("답변 전송 - 기타 예외")
    @WithCustomMockUser(username = "adminUser")
    fun testReplyError() {
        doThrow(RuntimeException("error")).whenever(service).adminReply(anyOrNull(), anyOrNull(), anyOrNull())
        mockMvc
            .perform(
                post("/api/v1/inquiry/admin/threads/$threadId/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(SendMessageRequest(body = "hi"))),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("읽음 처리 - 204")
    @WithCustomMockUser(username = "adminUser")
    fun testMarkRead() {
        mockMvc
            .perform(post("/api/v1/inquiry/admin/threads/$threadId/read"))
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("읽음 처리 - 404")
    @WithCustomMockUser(username = "adminUser")
    fun testMarkReadNotFound() {
        doThrow(InquiryThreadNotFoundException()).whenever(service).adminMarkRead(any())
        mockMvc
            .perform(post("/api/v1/inquiry/admin/threads/$threadId/read"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("THREAD_NOT_FOUND"))
    }

    @Test
    @DisplayName("읽음 처리 - 기타 예외")
    @WithCustomMockUser(username = "adminUser")
    fun testMarkReadError() {
        doThrow(RuntimeException("error")).whenever(service).adminMarkRead(any())
        mockMvc
            .perform(post("/api/v1/inquiry/admin/threads/$threadId/read"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("스레드 수정 - 200")
    @WithCustomMockUser(username = "adminUser")
    fun testUpdateThread() {
        doReturn(thread()).whenever(service).adminUpdateThread(anyOrNull(), anyOrNull(), anyOrNull())
        mockMvc
            .perform(
                patch("/api/v1/inquiry/admin/threads/$threadId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(PatchThreadRequest(status = "PENDING"))),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(threadId.toString()))
    }

    @Test
    @DisplayName("스레드 수정 - 404")
    @WithCustomMockUser(username = "adminUser")
    fun testUpdateThreadNotFound() {
        doThrow(InquiryThreadNotFoundException()).whenever(service).adminUpdateThread(anyOrNull(), anyOrNull(), anyOrNull())
        mockMvc
            .perform(
                patch("/api/v1/inquiry/admin/threads/$threadId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(PatchThreadRequest(status = "PENDING"))),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("THREAD_NOT_FOUND"))
    }

    @Test
    @DisplayName("스레드 수정 - 400 무효 상태")
    @WithCustomMockUser(username = "adminUser")
    fun testUpdateThreadInvalidStatus() {
        doThrow(InvalidInquiryStatusException()).whenever(service).adminUpdateThread(anyOrNull(), anyOrNull(), anyOrNull())
        mockMvc
            .perform(
                patch("/api/v1/inquiry/admin/threads/$threadId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(PatchThreadRequest(status = "CLOSED"))),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("INVALID_STATUS"))
    }

    @Test
    @DisplayName("스레드 수정 - 기타 예외")
    @WithCustomMockUser(username = "adminUser")
    fun testUpdateThreadError() {
        doThrow(RuntimeException("error")).whenever(service).adminUpdateThread(anyOrNull(), anyOrNull(), anyOrNull())
        mockMvc
            .perform(
                patch("/api/v1/inquiry/admin/threads/$threadId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(PatchThreadRequest(status = "PENDING"))),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    @DisplayName("문의 실시간 스트림(관리자) - 200 SSE 연결")
    @WithCustomMockUser(username = "adminUser")
    fun testStream() {
        mockMvc
            .perform(get("/api/v1/inquiry/admin/stream"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
    }

    @Test
    @DisplayName("스레드 종료 - 204")
    @WithCustomMockUser(username = "adminUser")
    fun testCloseThread() {
        mockMvc
            .perform(delete("/api/v1/inquiry/admin/threads/$threadId"))
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("스레드 종료 - 404")
    @WithCustomMockUser(username = "adminUser")
    fun testCloseThreadNotFound() {
        doThrow(InquiryThreadNotFoundException()).whenever(service).adminCloseThread(any())
        mockMvc
            .perform(delete("/api/v1/inquiry/admin/threads/$threadId"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("THREAD_NOT_FOUND"))
    }

    @Test
    @DisplayName("스레드 종료 - 기타 예외")
    @WithCustomMockUser(username = "adminUser")
    fun testCloseThreadError() {
        doThrow(RuntimeException("error")).whenever(service).adminCloseThread(any())
        mockMvc
            .perform(delete("/api/v1/inquiry/admin/threads/$threadId"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("INTERNAL_SERVER_ERROR"))
    }
}
