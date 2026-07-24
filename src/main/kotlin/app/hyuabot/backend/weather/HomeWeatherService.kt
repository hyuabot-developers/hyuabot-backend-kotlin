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
    val observedAt: ZonedDateTime? = null,
    val forecastUpdatedAt: ZonedDateTime? = null,
    val currentTemperature: Double? = null,
    val currentPrecipitationType: String? = null,
    val currentPrecipitationAmount: Double? = null,
    val minimumTemperature: Double? = null,
    val maximumTemperature: Double? = null,
    val precipitationProbabilityMax: Int,
    val precipitationStartAt: ZonedDateTime? = null,
    val precipitationEndAt: ZonedDateTime? = null,
    val precipitationType: String,
    val precipitationConfidence: String? = null,
    val availableModelCount: Int? = null,
    val agreeingModelCount: Int? = null,
    val primaryCondition: String,
    val attribution: String? = null,
    val sources: List<WeatherSourceStatus> = emptyList(),
)

data class WeatherSourceStatus(
    val source: String,
    val status: String,
)

@Service
class HomeWeatherService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val watchAnalyticsClock: Clock,
    @param:Value("\${weather.home.redis-key:weather:home:erica}") private val redisKey: String,
    @param:Value("\${weather.home.shadow-redis-key:weather:home:erica:shadow}") private val shadowRedisKey: String,
) {
    fun current(): HomeWeatherPayload? = read(redisKey)

    fun shadow(): HomeWeatherPayload? = read(shadowRedisKey)

    private fun read(key: String): HomeWeatherPayload? {
        val value = redisTemplate.opsForValue().get(key) ?: return null
        return runCatching { objectMapper.readValue(value, HomeWeatherPayload::class.java) }
            .getOrNull()
            ?.takeIf { it.expiresAt.isAfter(ZonedDateTime.now(watchAnalyticsClock)) }
    }
}
