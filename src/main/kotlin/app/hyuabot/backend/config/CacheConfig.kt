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
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import tools.jackson.module.kotlin.kotlinModule
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): RedisCacheManager {
        // Kotlin module so cached Kotlin data classes (e.g. codegen DTOs) round-trip;
        // default typing (restricted to our own DTO + collection types) so concrete element
        // types are reconstructed on read without enabling arbitrary polymorphic deserialization.
        val typeValidator =
            BasicPolymorphicTypeValidator
                .builder()
                .allowIfSubType("app.hyuabot.backend.codegen.types.")
                .allowIfSubType("java.util.")
                // Kotlin stdlib collections (e.g. EmptyList from emptyList()) appear as concrete
                // element/collection types in cached DTOs; allow them so empty lists round-trip.
                .allowIfSubType("kotlin.collections.")
                .build()
        val valueSerializer =
            GenericJacksonJsonRedisSerializer
                .builder { JsonMapper.builder().addModule(kotlinModule()) }
                .enableDefaultTyping(typeValidator)
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
