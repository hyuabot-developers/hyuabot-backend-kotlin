package app.hyuabot.backend.weather

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeWeatherServiceTest {
    private val redisTemplate = mock<RedisTemplate<String, String>>()
    private val valueOperations = mock<ValueOperations<String, String>>()
    private val objectMapper = mock<ObjectMapper>()
    private val now = Instant.parse("2026-07-21T03:00:00Z")
    private val service =
        HomeWeatherService(
            redisTemplate,
            objectMapper,
            Clock.fixed(now, ZoneOffset.UTC),
            "weather:test",
            "weather:test:shadow",
        ).also {
            whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
        }

    @Test
    fun `returns an unexpired forecast`() {
        val forecast = forecast(expiresAt = "2026-07-21T13:00:00+09:00")
        whenever(valueOperations.get("weather:test")).thenReturn("forecast-json")
        whenever(objectMapper.readValue("forecast-json", HomeWeatherPayload::class.java)).thenReturn(forecast)

        assertEquals(forecast, service.current())
    }

    @Test
    fun `returns an unexpired shadow forecast`() {
        val forecast = forecast(expiresAt = "2026-07-21T13:00:00+09:00")
        whenever(valueOperations.get("weather:test:shadow")).thenReturn("shadow-json")
        whenever(objectMapper.readValue("shadow-json", HomeWeatherPayload::class.java)).thenReturn(forecast)

        assertEquals(forecast, service.shadow())
    }

    @Test
    fun `hides missing malformed and expired forecasts`() {
        assertNull(service.current())

        whenever(valueOperations.get("weather:test")).thenReturn("malformed")
        whenever(objectMapper.readValue("malformed", HomeWeatherPayload::class.java))
            .thenThrow(IllegalArgumentException())
        assertNull(service.current())

        val expired = forecast(expiresAt = "2026-07-21T11:59:59+09:00")
        whenever(valueOperations.get("weather:test")).thenReturn("expired-json")
        whenever(objectMapper.readValue("expired-json", HomeWeatherPayload::class.java)).thenReturn(expired)
        assertNull(service.current())
    }

    @Test
    fun `uses null defaults for optional forecast values`() {
        val forecast =
            HomeWeatherPayload(
                issuedAt = ZonedDateTime.parse("2026-07-21T11:00:00+09:00"),
                expiresAt = ZonedDateTime.parse("2026-07-21T13:00:00+09:00"),
                precipitationProbabilityMax = 0,
                precipitationType = "NONE",
                primaryCondition = "CLEAR",
            )

        assertEquals(ZonedDateTime.parse("2026-07-21T11:00:00+09:00"), forecast.issuedAt)
        assertNull(forecast.currentTemperature)
        assertNull(forecast.minimumTemperature)
        assertNull(forecast.maximumTemperature)
        assertEquals(0, forecast.precipitationProbabilityMax)
        assertNull(forecast.precipitationStartAt)
        assertNull(forecast.observedAt)
        assertNull(forecast.forecastUpdatedAt)
        assertNull(forecast.currentPrecipitationType)
        assertNull(forecast.currentPrecipitationAmount)
        assertNull(forecast.precipitationEndAt)
        assertNull(forecast.precipitationConfidence)
        assertNull(forecast.availableModelCount)
        assertNull(forecast.agreeingModelCount)
        assertNull(forecast.attribution)
        assertEquals(emptyList(), forecast.sources)
        assertEquals("NONE", forecast.precipitationType)
        assertEquals("CLEAR", forecast.primaryCondition)
    }

    private fun forecast(expiresAt: String) =
        HomeWeatherPayload(
            issuedAt = ZonedDateTime.parse("2026-07-21T11:00:00+09:00"),
            expiresAt = ZonedDateTime.parse(expiresAt),
            currentTemperature = 31.0,
            minimumTemperature = 25.0,
            maximumTemperature = 34.0,
            precipitationProbabilityMax = 60,
            precipitationStartAt = ZonedDateTime.parse("2026-07-21T15:00:00+09:00"),
            precipitationType = "RAIN",
            primaryCondition = "RAIN",
        )
}
