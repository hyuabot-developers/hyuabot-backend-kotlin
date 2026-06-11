package app.hyuabot.backend.config

import io.micrometer.core.instrument.FunctionCounter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.support.NoOpCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.cache.RedisCache
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
    // Disable all caching when the `nocache` profile is active (used for load-test A/B baselines).
    @Bean
    @Profile("nocache")
    fun noOpCacheManager(): CacheManager = NoOpCacheManager()

    @Bean
    @Profile("!nocache")
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
        // Register every cache name at startup so Micrometer's RedisCacheMeterBinderProvider binds
        // each one (it only wires caches present when metrics are registered; lazily-created caches
        // would otherwise emit no cache_gets/puts metrics). Daily-static cafeteria menus can live
        // longer than the default.
        val perCache =
            mapOf(
                "cafeteriaMenu" to defaults.entryTtl(Duration.ofHours(1)),
                "subwayStation" to defaults,
                "subwayTimetable" to defaults,
            )
        return RedisCacheManager
            .builder(connectionFactory)
            // Collect per-cache hit/miss/put statistics so they surface as Micrometer cache_* metrics.
            .enableStatistics()
            .cacheDefaults(defaults)
            .withInitialCacheConfigurations(perCache)
            .build()
    }

    // Spring Boot 4.x CacheMetricsAutoConfiguration may not bind RedisCacheManager metrics
    // automatically. This bean explicitly registers cache_gets/puts/removals for each cache
    // after all singletons are initialized, ensuring Prometheus sees the metrics regardless.
    @Bean
    @Profile("!nocache")
    fun redisCacheMetricsBinder(
        meterRegistry: MeterRegistry,
        cacheManager: RedisCacheManager,
    ): SmartInitializingSingleton =
        SmartInitializingSingleton {
            cacheManager.cacheNames.forEach { name ->
                val cache = cacheManager.getCache(name) as? RedisCache ?: return@forEach
                FunctionCounter
                    .builder("cache.gets", cache) { it.statistics.hits.toDouble() }
                    .tag("cache", name)
                    .tag("result", "hit")
                    .register(meterRegistry)
                FunctionCounter
                    .builder("cache.gets", cache) { it.statistics.misses.toDouble() }
                    .tag("cache", name)
                    .tag("result", "miss")
                    .register(meterRegistry)
                FunctionCounter
                    .builder("cache.puts", cache) { it.statistics.puts.toDouble() }
                    .tag("cache", name)
                    .register(meterRegistry)
                FunctionCounter
                    .builder("cache.removals", cache) { it.statistics.deletes.toDouble() }
                    .tag("cache", name)
                    .register(meterRegistry)
            }
        }
}
