package app.hyuabot.backend.adminoverview

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

class PrometheusClientTest {
    @Test
    fun `cron job queries parse valid series and ignore malformed series`() {
        withServer(
            listOf(
                """{"status":"success","data":{"result":[{"metric":{"owner_name":"bus-realtime-cron-job"},"value":[1,"100"]},{"metric":{},"value":[1,"200"]},{"metric":{"owner_name":"invalid"},"value":[1,"not-a-number"]}]}}""",
                """{"status":"success","data":{"result":[{"metric":{"owner_name":"subway-realtime-cron-job"},"value":[1,"200"]}]}}""",
            ),
        ) { baseURL ->
            val runs = PrometheusClient(baseURL).getCronJobRuns()
            assertEquals("1970-01-01T00:01:40Z", runs.getValue("bus-realtime-cron-job").lastSuccessAt)
            assertEquals(null, runs.getValue("bus-realtime-cron-job").lastFailureAt)
            assertEquals(null, runs.getValue("subway-realtime-cron-job").lastSuccessAt)
            assertEquals("1970-01-01T00:03:20Z", runs.getValue("subway-realtime-cron-job").lastFailureAt)
        }
    }

    @Test
    fun `failed and empty Prometheus responses are rejected`() {
        withServer(listOf("""{"status":"error","data":{"result":[]}}""")) { baseURL ->
            assertThrows(IllegalStateException::class.java) { PrometheusClient(baseURL).getCronJobRuns() }
        }
        withServer(listOf("")) { baseURL ->
            assertThrows(IllegalStateException::class.java) { PrometheusClient(baseURL).getCronJobRuns() }
        }
    }

    private fun withServer(
        responses: List<String>,
        block: (String) -> Unit,
    ) {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        val requestIndex = AtomicInteger()
        server.createContext("/api/v1/query") { exchange ->
            val response = responses[requestIndex.getAndIncrement().coerceAtMost(responses.lastIndex)]
            val bytes = response.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            block("http://127.0.0.1:${server.address.port}")
        } finally {
            server.stop(0)
        }
    }
}
