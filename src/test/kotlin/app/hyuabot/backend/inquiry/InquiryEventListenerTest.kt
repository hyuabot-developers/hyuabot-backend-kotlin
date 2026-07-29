package app.hyuabot.backend.inquiry

import app.hyuabot.backend.inquiry.domain.InquiryEvent
import app.hyuabot.backend.inquiry.sse.InquiryEventListener
import app.hyuabot.backend.inquiry.sse.InquirySseRegistry
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.connection.Message
import tools.jackson.databind.ObjectMapper

class InquiryEventListenerTest {
    private val registry = mock<InquirySseRegistry>()
    private val objectMapper = ObjectMapper()
    private val listener = InquiryEventListener(registry, objectMapper)

    @Test
    @DisplayName("onMessage - 정상 JSON -> dispatch 호출")
    fun testOnMessageValid() {
        val event =
            InquiryEvent(
                kind = "thread",
                threadId = "t",
                installationId = "i",
                status = "OPEN",
            )
        val message = mock<Message>()
        whenever(message.body).thenReturn(objectMapper.writeValueAsBytes(event))
        listener.onMessage(message, null)
        verify(registry).dispatch(argThat { kind == "thread" && status == "OPEN" })
    }

    @Test
    @DisplayName("onMessage - 잘못된 JSON -> 예외 무시, dispatch 미호출")
    fun testOnMessageInvalid() {
        val message = mock<Message>()
        whenever(message.body).thenReturn("not-json".toByteArray())
        listener.onMessage(message, null)
        verify(registry, never()).dispatch(any())
    }
}
