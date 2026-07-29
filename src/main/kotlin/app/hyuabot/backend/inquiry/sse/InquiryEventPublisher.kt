package app.hyuabot.backend.inquiry.sse

import app.hyuabot.backend.inquiry.domain.InquiryEvent
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class InquiryEventPublisher(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    fun publish(event: InquiryEvent) {
        redisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(event))
    }

    companion object {
        const val CHANNEL = "inquiry:events"
    }
}
