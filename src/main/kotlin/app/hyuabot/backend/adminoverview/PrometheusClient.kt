package app.hyuabot.backend.adminoverview

import app.hyuabot.backend.adminoverview.domain.CronJobRun
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Instant

@Component
class PrometheusClient(
    @param:Value("\${admin.overview.prometheus-base-url:http://prometheus:9090}") baseURL: String,
) {
    private val restClient = RestClient.builder().baseUrl(baseURL).build()

    fun getCronJobRuns(): Map<String, CronJobRun> {
        val successes = query(LAST_SUCCESS_QUERY)
        val failures = query(LAST_FAILURE_QUERY)
        return (successes.keys + failures.keys).associateWith { name ->
            CronJobRun(
                lastSuccessAt = successes[name]?.toString(),
                lastFailureAt = failures[name]?.toString(),
            )
        }
    }

    private fun query(expression: String): Map<String, Instant> {
        val response =
            restClient
                .get()
                .uri("/api/v1/query?query={query}", expression)
                .retrieve()
                .body(PrometheusResponse::class.java)
                ?: throw IllegalStateException("Prometheus returned an empty response")
        if (response.status != "success") throw IllegalStateException("Prometheus query failed")
        return response.data.result
            .mapNotNull { result ->
                val name = result.metric["owner_name"] ?: return@mapNotNull null
                val seconds =
                    result.value
                        .getOrNull(1)
                        ?.toString()
                        ?.toDoubleOrNull() ?: return@mapNotNull null
                name to Instant.ofEpochSecond(seconds.toLong())
            }.toMap()
    }

    private data class PrometheusResponse(
        val status: String,
        val data: PrometheusData,
    )

    private data class PrometheusData(
        val result: List<PrometheusResult>,
    )

    private data class PrometheusResult(
        val metric: Map<String, String>,
        val value: List<Any>,
    )

    companion object {
        private const val LAST_SUCCESS_QUERY =
            "max by (owner_name) ((kube_job_status_completion_time{namespace=\"hyuabot\"} * on(job_name) group_left(owner_name) kube_job_owner{namespace=\"hyuabot\",owner_kind=\"CronJob\"}) * on(job_name) group_left() (kube_job_status_succeeded{namespace=\"hyuabot\"} > 0))"
        private const val LAST_FAILURE_QUERY =
            "max by (owner_name) ((kube_job_status_start_time{namespace=\"hyuabot\"} * on(job_name) group_left(owner_name) kube_job_owner{namespace=\"hyuabot\",owner_kind=\"CronJob\"}) * on(job_name) group_left() (kube_job_status_failed{namespace=\"hyuabot\"} > 0))"
    }
}
