package app.hyuabot.backend.adminoverview.domain

data class AdminOverviewResponse(
    val checkedAt: String,
    val services: List<AdminServiceStatus>,
    val weatherForecast: AdminWeatherForecastStatus?,
    val expiringInvitationCount: Int?,
    val grafanaURL: String,
)

data class AdminWeatherForecastStatus(
    val generatedAt: String,
    val observedAt: String?,
    val availableModelCount: Int,
    val agreeingModelCount: Int,
    val precipitationConfidence: String?,
    val sources: List<AdminWeatherSourceStatus>,
)

data class AdminWeatherSourceStatus(
    val source: String,
    val status: String,
)

data class AdminServiceStatus(
    val id: String,
    val title: String,
    val status: String,
    val message: String,
    val lastSuccessAt: String?,
    val lastFailureAt: String?,
    val managementPath: String,
)

data class CronJobRun(
    val lastSuccessAt: String?,
    val lastFailureAt: String?,
)
