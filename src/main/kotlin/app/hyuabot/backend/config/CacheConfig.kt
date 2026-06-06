package app.hyuabot.backend.config

import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): RedisCacheManager {
        // Kotlin module so cached Kotlin data classes (e.g. codegen DTOs) round-trip;
        // default typing so the concrete element types are reconstructed on read.
        val valueSerializer =
            GenericJacksonJsonRedisSerializer
                .builder { JsonMapper.builder().addModule(kotlinModule()) }
                .enableUnsafeDefaultTyping()
                .build()
        val defaults =
            RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()),
                ).serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer<Any>(valueSerializer),
                )
        // Daily-static cafeteria menus can live longer than the default.
        val perCache = mapOf("cafeteriaMenu" to defaults.entryTtl(Duration.ofHours(1)))
        return RedisCacheManager
            .builder(connectionFactory)
            .cacheDefaults(defaults)
            .withInitialCacheConfigurations(perCache)
            .build()
    }
}
