package app.hyuabot.backend.weather

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
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

    @Test
    fun `ignores fields added by weather collectors`() {
        val mapper =
            JsonMapper
                .builder()
                .addModule(kotlinModule())
                .build()
        val json =
            """{"issuedAt":"2026-07-21T11:00:00+09:00","expiresAt":"2026-07-21T13:00:00+09:00","precipitationProbabilityMax":0,"precipitationType":"NONE","primaryCondition":"CLEAR","humidity":43,"windSpeed":2.5,"snowAmount":0.5,"airQuality":{"pm10Value":20,"pm10Grade":1,"pm25Value":8,"pm25Grade":1,"khaiValue":35,"khaiGrade":1,"stationName":"Ansan","measuredAt":"2026-07-21T11:00:00+09:00","futureAirField":"ignored"},"warnings":[{"title":"호우주의보","kind":"호우","level":"주의보","area":"안산","issuedAt":"2026-07-21T11:00:00+09:00","futureWarningField":"ignored"}],"uvIndex":{"value":3,"grade":"낮음","forecastAt":"2026-07-21T11:00:00+09:00","futureUvField":"ignored"},"futureCollectorField":"ignored"}"""

        val payload = mapper.readValue(json, HomeWeatherPayload::class.java)

        assertEquals("CLEAR", payload.primaryCondition)
        assertEquals(43, payload.humidity)
        assertEquals(2.5, payload.windSpeed)
        assertEquals(0.5, payload.snowAmount)
        assertEquals(20, payload.airQuality?.pm10Value)
        assertEquals("호우", payload.warnings.single().kind)
        assertEquals("낮음", payload.uvIndex?.grade)
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
