package app.hyuabot.backend.weather

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery

@DgsComponent
class HomeWeatherDataFetcher(
    private val homeWeatherService: HomeWeatherService,
) {
    @DgsQuery
    fun homeWeather(): HomeWeatherPayload? = homeWeatherService.current()
}
