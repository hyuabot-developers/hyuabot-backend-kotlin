package app.hyuabot.backend.watchanalytics

data class WatchAnalyticsEventRequest(
    val event: String,
    val platform: String,
    val installationId: String,
    val appVersion: String,
    val entryPoint: String,
    val stopId: String? = null,
)
