package app.hyuabot.backend.config

import io.micrometer.core.instrument.FunctionCounter
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory

class CacheConfigTest {
    @Test
    @DisplayName("noOpCacheManager returns a NoOpCacheManager instance")
    fun noOpCacheManager() {
        val cacheConfig = CacheConfig()
        val cacheManager = cacheConfig.noOpCacheManager()
        assert(cacheManager.cacheNames.isEmpty())
    }

    @Test
    @DisplayName("cacheManager returns a RedisCacheManager with configured caches")
    fun cacheManager() {
        val cacheConfig = CacheConfig()
        val connectionFactory = mock<RedisConnectionFactory>()
        val cacheManager: RedisCacheManager = cacheConfig.cacheManager(connectionFactory)
        cacheManager.afterPropertiesSet()
        assert(cacheManager.cacheNames.containsAll(listOf("cafeteriaMenu", "subwayStation", "subwayTimetable")))
    }

    @Test
    @DisplayName("redisCacheMetricsBinder registers FunctionCounters for each cache's statistics")
    fun redisCacheMetricsBinder() {
        val cacheConfig = CacheConfig()
        val connectionFactory = mock<RedisConnectionFactory>()
        val cacheManager: RedisCacheManager = cacheConfig.cacheManager(connectionFactory)
        cacheManager.afterPropertiesSet()
        val meterRegistry = SimpleMeterRegistry()
        val binder: SmartInitializingSingleton = cacheConfig.redisCacheMetricsBinder(meterRegistry, cacheManager)
        binder.afterSingletonsInstantiated()
        // 3 caches × 4 counters (cache.gets hit/miss, cache.puts, cache.removals) = 12
        assert(meterRegistry.meters.size == 12)
        // invoke each counter's lambda to cover the ToDoubleFunction bodies
        meterRegistry.meters.filterIsInstance<FunctionCounter>().forEach { it.count() }
    }
}
