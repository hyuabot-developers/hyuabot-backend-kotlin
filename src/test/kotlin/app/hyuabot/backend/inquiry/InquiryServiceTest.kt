package app.hyuabot.backend.inquiry

import app.hyuabot.backend.database.entity.InquiryMessage
import app.hyuabot.backend.database.entity.InquiryThread
import app.hyuabot.backend.database.repository.InquiryMessageRepository
import app.hyuabot.backend.database.repository.InquiryThreadRepository
import app.hyuabot.backend.inquiry.domain.InquiryEvent
import app.hyuabot.backend.inquiry.exception.EmptyInquiryMessageException
import app.hyuabot.backend.inquiry.exception.InquiryThreadForbiddenException
import app.hyuabot.backend.inquiry.exception.InquiryThreadNotFoundException
import app.hyuabot.backend.inquiry.exception.InvalidInquiryStatusException
import app.hyuabot.backend.inquiry.push.InquiryPushService
import app.hyuabot.backend.inquiry.sse.InquiryEventPublisher
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

@ExtendWith(MockitoExtension::class)
class InquiryServiceTest {
    @Mock
    lateinit var threadRepository: InquiryThreadRepository

    @Mock
    lateinit var messageRepository: InquiryMessageRepository

    @Mock
    lateinit var eventPublisher: InquiryEventPublisher

    @Mock
    lateinit var pushService: InquiryPushService

    @InjectMocks
    lateinit var service: InquiryService

    private val installationId: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val otherInstallationId: UUID = UUID.fromString("99999999-9999-9999-9999-999999999999")
    private val threadId: UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")

    private fun thread(
        id: UUID = threadId,
        installation: UUID = installationId,
        entryScreen: String? = null,
        entryScreenName: String? = null,
        status: String = "OPEN",
        assignedAdminUserId: String? = null,
    ) = InquiryThread(
        id = id,
        installationId = installation,
        platform = "iOS",
        status = status,
        entryScreen = entryScreen,
        entryScreenName = entryScreenName,
        assignedAdminUserId = assignedAdminUserId,
        createdAt = ZonedDateTime.now(),
        updatedAt = ZonedDateTime.now(),
    )

    private fun message(
        id: Long? = 1L,
        senderType: String = "USER",
        readAt: ZonedDateTime? = null,
    ) = InquiryMessage(
        id = id,
        threadId = threadId,
        senderType = senderType,
        body = "body",
        readAt = readAt,
        createdAt = ZonedDateTime.now(),
    )

    @Test
    @DisplayName("openOrGetActiveThread - 활성 스레드 없음 -> 신규 생성")
    fun testOpenNewThread() {
        whenever(
            threadRepository.findFirstByInstallationIdAndStatusInOrderByCreatedAtDesc(installationId, InquiryService.ACTIVE_STATUSES),
        ).thenReturn(null)
        whenever(threadRepository.save(any<InquiryThread>())).thenAnswer { it.arguments[0] as InquiryThread }
        val result =
            service.openOrGetActiveThread(
                installationId = installationId,
                platform = "iOS",
                appVersion = "1.0.0",
                subject = "제목",
                contactEmail = "a@b.com",
                entryScreen = "shuttle.realtime",
                entryScreenName = "셔틀 실시간",
            )
        assertEquals(installationId, result.installationId)
        assertEquals("iOS", result.platform)
        assertEquals("OPEN", result.status)
        verify(threadRepository).save(any<InquiryThread>())
    }

    @Test
    @DisplayName("openOrGetActiveThread - 활성 스레드 존재 + entryScreen null -> 기존 반환")
    fun testOpenExistingThreadNoEntryScreen() {
        val existing = thread(entryScreen = "home")
        whenever(
            threadRepository.findFirstByInstallationIdAndStatusInOrderByCreatedAtDesc(installationId, InquiryService.ACTIVE_STATUSES),
        ).thenReturn(existing)
        val result =
            service.openOrGetActiveThread(installationId, "iOS", null, null, null, null, null)
        assertSame(existing, result)
        verify(messageRepository, never()).save(any())
        verify(threadRepository, never()).save(any())
    }

    @Test
    @DisplayName("openOrGetActiveThread - 활성 스레드 존재 + entryScreen 동일 -> 시스템 메시지 없음")
    fun testOpenExistingThreadSameEntryScreen() {
        val existing = thread(entryScreen = "home")
        whenever(
            threadRepository.findFirstByInstallationIdAndStatusInOrderByCreatedAtDesc(installationId, InquiryService.ACTIVE_STATUSES),
        ).thenReturn(existing)
        val result =
            service.openOrGetActiveThread(installationId, "iOS", null, null, null, "home", "홈")
        assertSame(existing, result)
        verify(messageRepository, never()).save(any())
    }

    @Test
    @DisplayName("openOrGetActiveThread - 활성 스레드 존재 + entryScreen 변경 -> 시스템 메시지 저장")
    fun testOpenExistingThreadDifferentEntryScreen() {
        val existing = thread(entryScreen = "home", entryScreenName = "홈")
        whenever(
            threadRepository.findFirstByInstallationIdAndStatusInOrderByCreatedAtDesc(installationId, InquiryService.ACTIVE_STATUSES),
        ).thenReturn(existing)
        whenever(messageRepository.save(any<InquiryMessage>())).thenAnswer { (it.arguments[0] as InquiryMessage).apply { id = 10L } }
        val result =
            service.openOrGetActiveThread(installationId, "iOS", null, null, null, "shuttle", "셔틀")
        assertSame(existing, result)
        assertEquals("shuttle", existing.entryScreen)
        assertEquals("셔틀", existing.entryScreenName)
        verify(threadRepository).save(existing)
        verify(messageRepository).save(
            argThat<InquiryMessage> {
                senderType == "SYSTEM" && body == InquiryService.entryScreenSystemMessage("셔틀")
            },
        )
        verify(eventPublisher).publish(
            argThat<InquiryEvent> {
                kind == "message" &&
                    installationId == this@InquiryServiceTest.installationId.toString()
            },
        )
    }

    @Test
    @DisplayName("openOrGetActiveThread - entryScreen 변경 + 이름 null -> 라우트 id로 시스템 메시지")
    fun testOpenExistingThreadDifferentEntryScreenWithoutName() {
        val existing = thread(entryScreen = "home", entryScreenName = "홈")
        whenever(
            threadRepository.findFirstByInstallationIdAndStatusInOrderByCreatedAtDesc(installationId, InquiryService.ACTIVE_STATUSES),
        ).thenReturn(existing)
        whenever(messageRepository.save(any<InquiryMessage>())).thenAnswer { (it.arguments[0] as InquiryMessage).apply { id = 11L } }
        service.openOrGetActiveThread(installationId, "iOS", null, null, null, "cafeteria", null)
        assertNull(existing.entryScreenName)
        verify(messageRepository).save(
            argThat<InquiryMessage> {
                senderType == "SYSTEM" && body == InquiryService.entryScreenSystemMessage("cafeteria")
            },
        )
    }

    @Test
    @DisplayName("getActiveThread - 값 반환")
    fun testGetActiveThread() {
        val existing = thread()
        whenever(
            threadRepository.findFirstByInstallationIdAndStatusInOrderByCreatedAtDesc(installationId, InquiryService.ACTIVE_STATUSES),
        ).thenReturn(existing)
        assertSame(existing, service.getActiveThread(installationId))
    }

    @Test
    @DisplayName("getActiveThread - null 반환")
    fun testGetActiveThreadNull() {
        whenever(
            threadRepository.findFirstByInstallationIdAndStatusInOrderByCreatedAtDesc(installationId, InquiryService.ACTIVE_STATUSES),
        ).thenReturn(null)
        assertNull(service.getActiveThread(installationId))
    }

    @Test
    @DisplayName("getMessages - after null")
    fun testGetMessagesWithoutAfter() {
        whenever(threadRepository.findById(threadId)).thenReturn(Optional.of(thread()))
        val messages = listOf(message())
        whenever(messageRepository.findByThreadIdOrderByCreatedAtAsc(threadId)).thenReturn(messages)
        assertEquals(messages, service.getMessages(threadId, installationId, null))
    }

    @Test
    @DisplayName("getMessages - after 지정")
    fun testGetMessagesWithAfter() {
        whenever(threadRepository.findById(threadId)).thenReturn(Optional.of(thread()))
        val messages = listOf(message(id = 5L))
        whenever(messageRepository.findByThreadIdAndIdGreaterThanOrderByCreatedAtAsc(threadId, 3L)).thenReturn(messages)
        assertEquals(messages, service.getMessages(threadId, installationId, 3L))
    }

    @Test
    @DisplayName("getMessages - 소유자가 아님")
    fun testGetMessagesForbidden() {
        whenever(threadRepository.findById(threadId)).thenReturn(Optional.of(thread(installation = otherInstallationId)))
        assertThrows<InquiryThreadForbiddenException> { service.getMessages(threadId, installationId, null) }
    }

    @Test
    @DisplayName("getMessages - 스레드 없음")
    fun testGetMessagesNotFound() {
        whenever(threadRepository.findById(threadId)).thenReturn(Optional.empty())
        assertThrows<InquiryThreadNotFoundException> { service.getMessages(threadId, installationId, null) }
    }

    @Test
    @DisplayName("sendUserMessage - 본문 공백")
    fun testSendUserMessageBlank() {
        assertThrows<EmptyInquiryMessageException> { service.sendUserMessage(threadId, installationId, "  ") }
        verify(messageRepository, never()).save(any())
        verify(threadRepository, never()).findById(any())
    }

    @Test
    @DisplayName("sendUserMessage - 정상 전송")
    fun testSendUserMessage() {
        val target = thread()
        whenever(threadRepository.findById(threadId)).thenReturn(Optional.of(target))
        whenever(messageRepository.save(any<InquiryMessage>())).thenAnswer { (it.arguments[0] as InquiryMessage).apply { id = 1L } }
        val result = service.sendUserMessage(threadId, installationId, "안녕하세요")
        assertEquals("USER", result.senderType)
        assertEquals("안녕하세요", result.body)
        assertNotNull(target.lastMessageAt)
        verify(threadRepository).save(target)
        verify(eventPublisher).publish(argThat<InquiryEvent> { kind == "message" && message?.senderType == "USER" })
    }

    @Test
    @DisplayName("sendUserMessage - 소유자가 아님")
    fun testSendUserMessageForbidden() {
        whenever(threadRepository.findById(threadId)).thenReturn(Optional.of(thread(installation = otherInstallationId)))
        assertThrows<InquiryThreadForbiddenException> { service.sendUserMessage(threadId, installationId, "hi") }
    }

    @Test
    @DisplayName("markReadByUser - ADMIN 미읽음 읽음 처리")
    fun testMarkReadByUser() {
        whenever(threadRepository.findById(threadId)).thenReturn(Optional.of(thread()))
        val unread = message(senderType = "ADMIN")
        whenever(messageRepository.findByThreadIdAndSenderTypeAndReadAtIsNull(threadId, "ADMIN")).thenReturn(listOf(unread))
        service.markReadByUser(threadId, installationId)
        assertNotNull(unread.readAt)
        verify(eventPublisher).publish(argThat<InquiryEvent> { kind == "read" && reader == "USER" })
    }

    @Test
    @DisplayName("markReadByUser - 소유자가 아님")
    fun testMarkReadByUserForbidden() {
        whenever(threadRepository.findById(threadId)).thenReturn(Optional.of(thread(installation = otherInstallationId)))
        assertThrows<InquiryThreadForbiddenException> { service.markReadByUser(threadId, installationId) }
    }

    @Test
    @DisplayName("adminListThreads - 배정 관리자 지정")
    fun testAdminListThreadsAssigned() {
        val threads = listOf(thread())
        whenever(
            threadRepository.findByAssignedAdminUserIdAndStatusInOrderByLastMessageAtDesc("adminUser", InquiryService.ACTIVE_STATUSES),
        ).thenReturn(threads)
        assertEquals(threads, service.adminListThreads("adminUser"))
    }

    @Test
    @DisplayName("adminListThreads - 전체")
    fun testAdminListThreadsAll() {
        val threads = listOf(thread())
        whenever(threadRepository.findByStatusInOrderByLastMessageAtDesc(InquiryService.ACTIVE_STATUSES)).thenReturn(threads)
        assertEquals(threads, service.adminListThreads(null))
    }

    @Test
    @DisplayName("adminGetThread - 존재")
    fun testAdminGetThread() {
        val target = thread()
        whenever(threadRepository.findById(threadId)).thenReturn(Optional.of(target))
        assertSame(target, service.adminGetThread(threadId))
    }

    @Test
    @DisplayName("adminGetThread - 없음")
    fun testAdminGetThreadNotFound() {
        whenever(threadRepository.findById(threadId)).thenReturn(Optional.empty())
        assertThrows<InquiryThreadNotFoundException> { service.adminGetThread(threadId) }
    }

    @Test
    @DisplayName("getMessagesForAdmin - 목록 반환")
    fun testGetMessagesForAdmin() {
        val messages = listOf(message())
        whenever(messageRepository.findByThreadIdOrderByCreatedAtAsc(threadId)).thenReturn(messages)
        assertEquals(messages, service.getMessagesForAdmin(threadId))
    }

    @Test
    @DisplayName("adminReply - 본문 공백")
    fun testAdminReplyBlank() {
        assertThrows<EmptyInquiryMessageException> { service.adminReply(threadId, "adminUser", " ") }
        verify(messageRepository, never()).save(any())
    }

    @Test
    @DisplayName("adminReply - 정상 답변")
    fun testAdminReply() {
        val target = thread()
        whenever(threadRepository.findById(threadId)).thenReturn(Optional.of(target))
        whenever(messageRepository.save(any<InquiryMessage>())).thenAnswer { (it.arguments[0] as InquiryMessage).apply { id = 1L } }
        val result = service.adminReply(threadId, "adminUser", "답변입니다")
        assertEquals("ADMIN", result.senderType)
        assertEquals("adminUser", result.senderAdminUserId)
        assertEquals("답변입니다", result.body)
        verify(threadRepository).save(target)
        verify(eventPublisher).publish(argThat<InquiryEvent> { kind == "message" && message?.senderType == "ADMIN" })
    }

    @Test
    @DisplayName("adminReply - 푸시 실패에도 답변 저장")
    fun testAdminReplyPushFailure() {
        val target = thread()
        whenever(threadRepository.findById(threadId)).thenReturn(Optional.of(target))
        whenever(messageRepository.save(any<InquiryMessage>())).thenAnswer { (it.arguments[0] as InquiryMessage).apply { id = 1L } }
        doThrow(RuntimeException("push failure")).whenever(pushService).notifyAdminMessage(any(), any(), any(), any())
        assertEquals("ADMIN", service.adminReply(threadId, "adminUser", "답변").senderType)
    }

    @Test
    @DisplayName("adminReply - 스레드 없음")
    fun testAdminReplyNotFound() {
        whenever(threadRepository.findById(threadId)).thenReturn(Optional.empty())
        assertThrows<InquiryThreadNotFoundException> { service.adminReply(threadId, "adminUser", "hi") }
    }

    @Test
    @DisplayName("adminMarkRead - USER 미읽음 읽음 처리")
    fun testAdminMarkRead() {
        whenever(threadRepository.findById(threadId)).thenReturn(Optional.of(thread()))
        val unread = message(senderType = "USER")
        whenever(messageRepository.findByThreadIdAndSenderTypeAndReadAtIsNull(threadId, "USER")).thenReturn(listOf(unread))
        service.adminMarkRead(threadId)
        assertNotNull(unread.readAt)
        verify(eventPublisher).publish(argThat<InquiryEvent> { kind == "read" && reader == "ADMIN" })
    }

    @Test
    @DisplayName("adminMarkRead - 스레드 없음")
    fun testAdminMarkReadNotFound() {
        whenever(threadRepository.findById(threadId)).thenReturn(Optional.empty())
        assertThrows<InquiryThreadNotFoundException> { service.adminMarkRead(threadId) }
    }

    @Test
    @DisplayName("adminUpdateThread - 변경 없음")
    fun testAdminUpdateThreadNoChange() {
        val target = thread()
        whenever(threadRepository.findById(threadId)).thenReturn(Optional.of(target))
        whenever(threadRepository.save(any<InquiryThread>())).thenAnswer { it.arguments[0] as InquiryThread }
        val result = service.adminUpdateThread(threadId, null, null)
        assertSame(target, result)
    }

    @Test
    @DisplayName("adminUpdateThread - 유효 상태")
    fun testAdminUpdateThreadValidStatus() {
        val target = thread()
        whenever(threadRepository.findById(threadId)).thenReturn(Optional.of(target))
        whenever(threadRepository.save(any<InquiryThread>())).thenAnswer { it.arguments[0] as InquiryThread }
        val result = service.adminUpdateThread(threadId, "PENDING", null)
        assertEquals("PENDING", result.status)
        verify(eventPublisher).publish(argThat<InquiryEvent> { kind == "thread" && status == "PENDING" })
    }

    @Test
    @DisplayName("adminUpdateThread - 무효 상태")
    fun testAdminUpdateThreadInvalidStatus() {
        whenever(threadRepository.findById(threadId)).thenReturn(Optional.of(thread()))
        assertThrows<InvalidInquiryStatusException> { service.adminUpdateThread(threadId, "CLOSED", null) }
    }

    @Test
    @DisplayName("adminUpdateThread - 배정 관리자 지정")
    fun testAdminUpdateThreadAssign() {
        val target = thread()
        whenever(threadRepository.findById(threadId)).thenReturn(Optional.of(target))
        whenever(threadRepository.save(any<InquiryThread>())).thenAnswer { it.arguments[0] as InquiryThread }
        val result = service.adminUpdateThread(threadId, null, "adminUser")
        assertEquals("adminUser", result.assignedAdminUserId)
    }

    @Test
    @DisplayName("adminCloseThread - 존재 시 삭제")
    fun testAdminCloseThread() {
        val target = thread()
        whenever(threadRepository.findById(threadId)).thenReturn(Optional.of(target))
        service.adminCloseThread(threadId)
        verify(threadRepository).delete(target)
        verify(eventPublisher).publish(argThat<InquiryEvent> { kind == "thread" && status == "CLOSED" })
    }

    @Test
    @DisplayName("adminCloseThread - 스레드 없음")
    fun testAdminCloseThreadNotFound() {
        whenever(threadRepository.findById(threadId)).thenReturn(Optional.empty())
        assertThrows<InquiryThreadNotFoundException> { service.adminCloseThread(threadId) }
    }

    @Test
    @DisplayName("companion - entryScreenSystemMessage")
    fun testEntryScreenSystemMessage() {
        assertEquals("사용자가 '셔틀' 화면에서 문의를 시작했습니다.", InquiryService.entryScreenSystemMessage("셔틀"))
    }
}
