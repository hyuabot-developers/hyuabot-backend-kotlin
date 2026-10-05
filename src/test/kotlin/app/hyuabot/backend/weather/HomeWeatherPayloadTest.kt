package app.hyuabot.backend.weather

import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeWeatherPayloadTest {
    @Test
    fun `payloads expose values and their default constructor values`() {
        val time = ZonedDateTime.parse("2026-10-05T12:00:00+09:00[Asia/Seoul]")

        val warning = HomeWeatherWarningPayload()
        assertEquals(null, warning.title)
        assertEquals(null, warning.issuedAt)
        assertEquals(null, warning.kind)
        assertEquals(null, warning.level)
        assertEquals(null, warning.area)
        assertEquals(null, warning.startsAt)
        assertEquals(null, warning.endsAt)

        val populatedWarning = HomeWeatherWarningPayload("warning", time, "RAIN", "high", "Seoul", time, time.plusHours(1))
        assertEquals("warning", populatedWarning.title)
        assertEquals(time, populatedWarning.issuedAt)
        assertEquals("RAIN", populatedWarning.kind)
        assertEquals("high", populatedWarning.level)
        assertEquals("Seoul", populatedWarning.area)
        assertEquals(time, populatedWarning.startsAt)
        assertEquals(time.plusHours(1), populatedWarning.endsAt)

        val uvIndex = HomeUvIndexPayload()
        assertEquals(null, uvIndex.value)
        assertEquals(null, uvIndex.grade)
        assertEquals(null, uvIndex.forecastAt)

        val populatedUvIndex = HomeUvIndexPayload(7, "high", time)
        assertEquals(7, populatedUvIndex.value)
        assertEquals("high", populatedUvIndex.grade)
        assertEquals(time, populatedUvIndex.forecastAt)

        val airQuality = HomeAirQualityPayload()
        assertEquals(null, airQuality.pm10Value)
        assertEquals(null, airQuality.pm10Grade)
        assertEquals(null, airQuality.pm25Value)
        assertEquals(null, airQuality.pm25Grade)
        assertEquals(null, airQuality.khaiValue)
        assertEquals(null, airQuality.khaiGrade)
        assertEquals(null, airQuality.stationName)
        assertEquals(null, airQuality.measuredAt)

        val populatedAirQuality = HomeAirQualityPayload(10, 1, 5, 2, 15, 1, "station", time)
        assertEquals(10, populatedAirQuality.pm10Value)
        assertEquals(1, populatedAirQuality.pm10Grade)
        assertEquals(5, populatedAirQuality.pm25Value)
        assertEquals(2, populatedAirQuality.pm25Grade)
        assertEquals(15, populatedAirQuality.khaiValue)
        assertEquals(1, populatedAirQuality.khaiGrade)
        assertEquals("station", populatedAirQuality.stationName)
        assertEquals(time, populatedAirQuality.measuredAt)
    }
}
