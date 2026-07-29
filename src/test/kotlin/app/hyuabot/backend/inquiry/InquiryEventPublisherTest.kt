package app.hyuabot.backend.inquiry

import app.hyuabot.backend.inquiry.domain.InquiryEvent
import app.hyuabot.backend.inquiry.sse.InquiryEventPublisher
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.data.redis.core.RedisTemplate
import tools.jackson.databind.ObjectMapper

class InquiryEventPublisherTest {
    private val redisTemplate = mock<RedisTemplate<String, String>>()
    private val objectMapper = ObjectMapper()
    private val publisher = InquiryEventPublisher(redisTemplate, objectMapper)

    @Test
    @DisplayName("publish - Redis 채널로 이벤트 직렬화 전송")
    fun testPublish() {
        val event =
            InquiryEvent(
                kind = "read",
                threadId = "22222222-2222-2222-2222-222222222222",
                installationId = "11111111-1111-1111-1111-111111111111",
                reader = "USER",
            )
        publisher.publish(event)
        verify(redisTemplate).convertAndSend(
            InquiryEventPublisher.CHANNEL,
            objectMapper.writeValueAsString(event),
        )
    }
}
