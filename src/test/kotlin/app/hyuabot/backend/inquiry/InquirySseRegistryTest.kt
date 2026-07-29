package app.hyuabot.backend.inquiry

import app.hyuabot.backend.inquiry.domain.InquiryEvent
import app.hyuabot.backend.inquiry.sse.InquirySseRegistry
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class InquirySseRegistryTest {
    private fun registryWithEmitter(emitter: SseEmitter) =
        object : InquirySseRegistry(ObjectMapper()) {
            override fun createEmitter(timeoutMs: Long): SseEmitter = emitter
        }

    @Test
    @DisplayName("registerForInstallation - 등록 및 콜백에서 제거")
    fun testRegisterForInstallation() {
        val emitter = mock<SseEmitter>()
        val registry = registryWithEmitter(emitter)
        val result = registry.registerForInstallation("inst")
        assertSame(emitter, result)
        assertEquals(1, registry.installationEmitters["inst"]!!.size)
        verify(emitter).send(any<SseEmitter.SseEventBuilder>())

        val completion = argumentCaptor<Runnable>()
        verify(emitter).onCompletion(completion.capture())
        val timeout = argumentCaptor<Runnable>()
        verify(emitter).onTimeout(timeout.capture())
        val error = argumentCaptor<Consumer<Throwable>>()
        verify(emitter).onError(error.capture())

        completion.firstValue.run()
        timeout.firstValue.run()
        error.firstValue.accept(RuntimeException("boom"))
        assertTrue(registry.installationEmitters["inst"]!!.isEmpty())
    }

    @Test
    @DisplayName("registerForAdmin - 등록 및 콜백에서 제거")
    fun testRegisterForAdmin() {
        val emitter = mock<SseEmitter>()
        val registry = registryWithEmitter(emitter)
        val result = registry.registerForAdmin()
        assertSame(emitter, result)
        assertEquals(1, registry.adminEmitters.size)
        verify(emitter).send(any<SseEmitter.SseEventBuilder>())

        val completion = argumentCaptor<Runnable>()
        verify(emitter).onCompletion(completion.capture())
        val timeout = argumentCaptor<Runnable>()
        verify(emitter).onTimeout(timeout.capture())
        val error = argumentCaptor<Consumer<Throwable>>()
        verify(emitter).onError(error.capture())

        completion.firstValue.run()
        timeout.firstValue.run()
        error.firstValue.accept(RuntimeException("boom"))
        assertTrue(registry.adminEmitters.isEmpty())
    }

    @Test
    @DisplayName("register - 초기 이벤트 전송 실패는 무시")
    fun testRegisterSendInitFailureIsCaught() {
        val emitter = mock<SseEmitter>()
        doThrow(RuntimeException("boom")).whenever(emitter).send(any<SseEmitter.SseEventBuilder>())
        val registry = registryWithEmitter(emitter)
        registry.registerForAdmin()
        assertEquals(1, registry.adminEmitters.size)
    }

    @Test
    @DisplayName("createEmitter - 실제 SseEmitter 생성")
    fun testCreateRealEmitter() {
        val registry = InquirySseRegistry(ObjectMapper())
        val emitter = registry.registerForInstallation("real")
        assertNotNull(emitter)
        assertEquals(1, registry.installationEmitters["real"]!!.size)
    }

    @Test
    @DisplayName("dispatch - 설치/관리자 전송 및 미등록 설치 분기")
    fun testDispatchSuccessAndUnknownInstallation() {
        val registry = InquirySseRegistry(ObjectMapper())
        val instEmitter = mock<SseEmitter>()
        registry.installationEmitters["inst"] = CopyOnWriteArrayList<SseEmitter>().apply { add(instEmitter) }
        val adminEmitter = mock<SseEmitter>()
        registry.adminEmitters.add(adminEmitter)

        registry.dispatch(InquiryEvent(kind = "message", threadId = "t", installationId = "inst"))
        verify(instEmitter).send(any<SseEmitter.SseEventBuilder>())
        verify(adminEmitter).send(any<SseEmitter.SseEventBuilder>())

        registry.dispatch(InquiryEvent(kind = "read", threadId = "t", installationId = "unknown", reader = "ADMIN"))
        verify(instEmitter).send(any<SseEmitter.SseEventBuilder>())
        verify(adminEmitter, times(2)).send(any<SseEmitter.SseEventBuilder>())
    }

    @Test
    @DisplayName("dispatch - 전송 실패 시 emitter 제거")
    fun testDispatchRemovesFailingEmitter() {
        val registry = InquirySseRegistry(ObjectMapper())
        val failing = mock<SseEmitter>()
        doThrow(RuntimeException("x")).whenever(failing).send(any<SseEmitter.SseEventBuilder>())
        registry.adminEmitters.add(failing)

        registry.dispatch(InquiryEvent(kind = "thread", threadId = "t", installationId = "i", status = "CLOSED"))
        verify(failing).completeWithError(any())
        assertTrue(registry.adminEmitters.isEmpty())
    }
}
