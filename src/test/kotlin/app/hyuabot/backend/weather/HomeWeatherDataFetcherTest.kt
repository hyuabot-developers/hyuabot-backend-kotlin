package app.hyuabot.backend.weather

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import kotlin.test.assertEquals

class HomeWeatherDataFetcherTest {
    private val service = mock<HomeWeatherService>()
    private val dataFetcher = HomeWeatherDataFetcher(service)

    @Test
    fun `delegates the home weather query to the service`() {
        val forecast =
            HomeWeatherPayload(
                issuedAt = ZonedDateTime.parse("2026-07-21T11:00:00+09:00"),
                expiresAt = ZonedDateTime.parse("2026-07-21T13:00:00+09:00"),
                precipitationProbabilityMax = 0,
                precipitationType = "NONE",
                primaryCondition = "CLEAR",
            )
        whenever(service.current()).thenReturn(forecast)

        assertEquals(forecast, dataFetcher.homeWeather())
        verify(service).current()
    }
}
