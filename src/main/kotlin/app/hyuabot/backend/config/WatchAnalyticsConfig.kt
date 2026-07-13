package app.hyuabot.backend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class WatchAnalyticsConfig {
    @Bean
    fun watchAnalyticsClock(): Clock = Clock.systemUTC()
}
