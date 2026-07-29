package app.hyuabot.backend.config

import app.hyuabot.backend.inquiry.sse.InquiryEventListener
import app.hyuabot.backend.inquiry.sse.InquiryEventPublisher
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig(
    @param:Value("\${spring.data.redis.host}") private val host: String,
    @param:Value("\${spring.data.redis.port}") private val port: Int,
) {
    @Bean
    fun redisConnectionFactory(): LettuceConnectionFactory = LettuceConnectionFactory(host, port)

    @Bean
    fun redisTemplate(): RedisTemplate<String, String> =
        RedisTemplate<String, String>().apply {
            connectionFactory = redisConnectionFactory()
            keySerializer = StringRedisSerializer()
            valueSerializer = StringRedisSerializer()
            hashKeySerializer = StringRedisSerializer()
        }

    @Bean
    fun redisMessageListenerContainer(inquiryEventListener: InquiryEventListener): RedisMessageListenerContainer =
        RedisMessageListenerContainer().apply {
            setConnectionFactory(redisConnectionFactory())
            addMessageListener(inquiryEventListener, ChannelTopic(InquiryEventPublisher.CHANNEL))
        }
}
