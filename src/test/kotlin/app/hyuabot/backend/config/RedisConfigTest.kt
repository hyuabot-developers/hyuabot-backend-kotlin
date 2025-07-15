package app.hyuabot.backend.config

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.redis.serializer.StringRedisSerializer

class RedisConfigTest {
    @Test
    @DisplayName("Test method that provides Redis connection factory")
    fun redisConnectionFactory() {
        val redisConfig = RedisConfig("localhost", 6379)
        val connectionFactory = redisConfig.redisConnectionFactory()
        assert(connectionFactory.hostName == "localhost")
        assert(connectionFactory.port == 6379)
    }

    @Test
    @DisplayName("Test method that provides Redis template")
    fun redisTemplate() {
        val redisConfig = RedisConfig("localhost", 6379)
        val redisTemplate = redisConfig.redisTemplate()
        assert(redisTemplate.keySerializer is StringRedisSerializer)
        assert(redisTemplate.valueSerializer is StringRedisSerializer)
        assert(redisTemplate.hashKeySerializer is StringRedisSerializer)
    }
}
