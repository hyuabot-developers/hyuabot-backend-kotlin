package app.hyuabot.backend.adminoverview

import app.hyuabot.backend.adminoverview.domain.AdminOverviewResponse
import app.hyuabot.backend.adminoverview.domain.AdminServiceStatus
import app.hyuabot.backend.adminoverview.domain.AdminWeatherForecastStatus
import app.hyuabot.backend.adminoverview.domain.AdminWeatherSourceStatus
import app.hyuabot.backend.adminoverview.domain.CronJobRun
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class AdminOverviewModelsTest {
    @Test
    fun `overview models expose value semantics`() {
        val status = AdminServiceStatus("id", "title", "NORMAL", "message", "success", "failure", "/path")
        val source = AdminWeatherSourceStatus("JMA_MSM", "AVAILABLE")
        val forecast = AdminWeatherForecastStatus("generated", "observed", 3, 2, "MEDIUM", listOf(source))
        val overview = AdminOverviewResponse("now", listOf(status), forecast, 1, "grafana")
        val run = CronJobRun("success", "failure")
        val (id, title, state, message, success, failure, path) = status
        val (checkedAt, services, weather, invitations, grafana) = overview
        val (generatedAt, observedAt, available, agreeing, confidence, sources) = forecast
        val (sourceName, sourceStatus) = source
        val (runSuccess, runFailure) = run
        assertEquals(
            listOf("id", "title", "NORMAL", "message", "success", "failure", "/path"),
            listOf(id, title, state, message, success, failure, path),
        )
        assertEquals(listOf("now", listOf(status), forecast, 1, "grafana"), listOf(checkedAt, services, weather, invitations, grafana))
        assertEquals(
            listOf("generated", "observed", 3, 2, "MEDIUM", listOf(source)),
            listOf(generatedAt, observedAt, available, agreeing, confidence, sources),
        )
        assertEquals(listOf("JMA_MSM", "AVAILABLE"), listOf(sourceName, sourceStatus))
        assertEquals(listOf("success", "failure"), listOf(runSuccess, runFailure))
        assertEquals("title", status.title)
        assertEquals("JMA_MSM", source.source)
        assertEquals("AVAILABLE", source.status)
        assertEquals("success", status.lastSuccessAt)
        assertEquals("failure", status.lastFailureAt)
        assertEquals("/path", status.managementPath)
        assertEquals("now", overview.checkedAt)
        assertEquals(status, status.copy())
        assertEquals(overview, overview.copy())
        assertEquals(forecast, forecast.copy())
        assertEquals(source, source.copy())
        assertEquals(run, run.copy())
        exerciseValue(
            status,
            status.copy(
                id = "other",
                title = "other",
                status = "ERROR",
                message = "other",
                lastSuccessAt = null,
                lastFailureAt = null,
                managementPath = "/other",
            ),
        )
        exerciseValue(
            overview,
            overview.copy(
                checkedAt = "later",
                services = emptyList(),
                weatherForecast = null,
                expiringInvitationCount = null,
                grafanaURL = "other",
            ),
        )
        exerciseValue(
            forecast,
            forecast.copy(
                generatedAt = "later",
                observedAt = null,
                availableModelCount = 1,
                agreeingModelCount = 0,
                precipitationConfidence = null,
                sources = emptyList(),
            ),
        )
        exerciseValue(source, source.copy(source = "GFS_GLOBAL", status = "FAILED"))
        exerciseValue(run, run.copy(lastSuccessAt = null, lastFailureAt = null))
    }

    private fun exerciseValue(
        value: Any,
        different: Any,
    ) {
        assertEquals(value, value)
        assertNotEquals(value, different)
        value.hashCode()
        value.toString()
    }
}
