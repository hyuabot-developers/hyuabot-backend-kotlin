package app.hyuabot.backend.adminoverview

import app.hyuabot.backend.adminoverview.domain.AdminOverviewResponse
import app.hyuabot.backend.adminoverview.domain.AdminServiceStatus
import app.hyuabot.backend.adminoverview.domain.CronJobRun
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class AdminOverviewModelsTest {
    @Test
    fun `overview models expose value semantics`() {
        val status = AdminServiceStatus("id", "title", "NORMAL", "message", "success", "failure", "/path")
        val overview = AdminOverviewResponse("now", listOf(status), 1, "grafana")
        val run = CronJobRun("success", "failure")
        val (id, title, state, message, success, failure, path) = status
        val (checkedAt, services, invitations, grafana) = overview
        val (runSuccess, runFailure) = run
        assertEquals(
            listOf("id", "title", "NORMAL", "message", "success", "failure", "/path"),
            listOf(id, title, state, message, success, failure, path),
        )
        assertEquals(listOf("now", listOf(status), 1, "grafana"), listOf(checkedAt, services, invitations, grafana))
        assertEquals(listOf("success", "failure"), listOf(runSuccess, runFailure))
        assertEquals("title", status.title)
        assertEquals("success", status.lastSuccessAt)
        assertEquals("failure", status.lastFailureAt)
        assertEquals("/path", status.managementPath)
        assertEquals("now", overview.checkedAt)
        assertEquals(status, status.copy())
        assertEquals(overview, overview.copy())
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
            overview.copy(checkedAt = "later", services = emptyList(), expiringInvitationCount = null, grafanaURL = "other"),
        )
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
