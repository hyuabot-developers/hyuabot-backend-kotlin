package app.hyuabot.backend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@Configuration
class LiveActivitySchedulerConfig {
    @Bean
    fun liveActivityTaskScheduler(): TaskScheduler =
        ThreadPoolTaskScheduler().apply {
            setPoolSize(2)
            setThreadNamePrefix("live-activity-")
            initialize()
        }
}
