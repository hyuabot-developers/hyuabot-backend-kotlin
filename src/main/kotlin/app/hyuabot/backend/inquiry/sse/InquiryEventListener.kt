package app.hyuabot.backend.inquiry.sse

import app.hyuabot.backend.inquiry.domain.InquiryEvent
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class InquiryEventListener(
    private val registry: InquirySseRegistry,
    private val objectMapper: ObjectMapper,
) : MessageListener {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun onMessage(
        message: Message,
        pattern: ByteArray?,
    ) {
        try {
            val event = objectMapper.readValue(message.body, InquiryEvent::class.java)
            registry.dispatch(event)
        } catch (e: Exception) {
            logger.warn("Failed to handle inquiry event", e)
        }
    }
}
