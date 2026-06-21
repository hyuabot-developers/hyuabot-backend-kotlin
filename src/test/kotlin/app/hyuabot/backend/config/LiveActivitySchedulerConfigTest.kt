package app.hyuabot.backend.config

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import kotlin.test.assertIs

class LiveActivitySchedulerConfigTest {
    @Test
    @DisplayName("Creates Live Activity task scheduler")
    fun liveActivityTaskScheduler() {
        val scheduler = LiveActivitySchedulerConfig().liveActivityTaskScheduler()

        assertIs<ThreadPoolTaskScheduler>(scheduler)
        scheduler.shutdown()
    }
}
