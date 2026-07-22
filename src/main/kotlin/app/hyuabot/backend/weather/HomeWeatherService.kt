package app.hyuabot.backend.weather

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.ZonedDateTime

data class HomeWeatherPayload(
    val issuedAt: ZonedDateTime,
    val expiresAt: ZonedDateTime,
    val currentTemperature: Double? = null,
    val minimumTemperature: Double? = null,
    val maximumTemperature: Double? = null,
    val precipitationProbabilityMax: Int,
    val precipitationStartAt: ZonedDateTime? = null,
    val precipitationType: String,
    val primaryCondition: String,
)

@Service
class HomeWeatherService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val watchAnalyticsClock: Clock,
    @param:Value("\${weather.home.redis-key:weather:home:erica}") private val redisKey: String,
) {
    fun current(): HomeWeatherPayload? {
        val value = redisTemplate.opsForValue().get(redisKey) ?: return null
        return runCatching { objectMapper.readValue(value, HomeWeatherPayload::class.java) }
            .getOrNull()
            ?.takeIf { it.expiresAt.isAfter(ZonedDateTime.now(watchAnalyticsClock)) }
    }
}
