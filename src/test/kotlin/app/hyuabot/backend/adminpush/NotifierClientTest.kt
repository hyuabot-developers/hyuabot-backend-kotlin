package app.hyuabot.backend.adminpush

import app.hyuabot.backend.adminpush.domain.AdminPushSubscriptionKeys
import app.hyuabot.backend.adminpush.domain.AdminPushSubscriptionRequest
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class NotifierClientTest {
    @Test
    fun `client forwards authenticated subscription requests`() {
        val requests = mutableListOf<CapturedRequest>()
        withServer(requests) { baseURL ->
            val client = NotifierClient(baseURL, "secret-token")
            assertEquals("vapid-public", client.getPublicKey().publicKey)
            assertTrue(client.getStatus("jil8885", "https://push.example/device").enabled)

            val request =
                AdminPushSubscriptionRequest(
                    endpoint = "https://push.example/device",
                    keys = AdminPushSubscriptionKeys("p256dh", "auth"),
                )
            client.subscribe("jil8885", "Safari", request)
            client.unsubscribe("jil8885", request.endpoint)
        }

        assertEquals(listOf("GET", "GET", "POST", "DELETE"), requests.map { it.method })
        assertTrue(requests.all { it.authorization == "Bearer secret-token" })
        assertTrue(URLDecoder.decode(requests[1].query, StandardCharsets.UTF_8).contains("userId=jil8885"))
        assertTrue(URLDecoder.decode(requests[1].query, StandardCharsets.UTF_8).contains("endpoint=https://push.example/device"))
        assertTrue(requests[2].body.contains("\"userId\":\"jil8885\""))
        assertTrue(requests[2].body.contains("\"userAgent\":\"Safari\""))
        assertTrue(requests[3].body.contains("\"endpoint\":\"https://push.example/device\""))
    }

    @Test
    fun `empty notifier responses are rejected`() {
        withServer(mutableListOf(), emptyResponses = true) { baseURL ->
            val client = NotifierClient(baseURL, "token")
            assertThrows(IllegalArgumentException::class.java) { client.getPublicKey() }
            assertThrows(IllegalArgumentException::class.java) { client.getStatus("user", "endpoint") }
        }
    }

    private fun withServer(
        requests: MutableList<CapturedRequest>,
        emptyResponses: Boolean = false,
        block: (String) -> Unit,
    ) {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/") { exchange ->
            requests += exchange.capture()
            val response =
                when {
                    emptyResponses -> ""
                    exchange.requestURI.path.endsWith("public-key") -> """{"publicKey":"vapid-public"}"""
                    exchange.requestURI.path.endsWith("status") -> """{"enabled":true}"""
                    else -> ""
                }
            if (response.isEmpty()) {
                exchange.sendResponseHeaders(204, -1)
            } else {
                val bytes = response.toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            exchange.close()
        }
        server.start()
        try {
            block("http://127.0.0.1:${server.address.port}")
        } finally {
            server.stop(0)
        }
    }

    private fun HttpExchange.capture() =
        CapturedRequest(
            method = requestMethod,
            authorization = requestHeaders.getFirst("Authorization"),
            query = requestURI.rawQuery.orEmpty(),
            body = requestBody.bufferedReader().readText(),
        )

    private data class CapturedRequest(
        val method: String,
        val authorization: String?,
        val query: String,
        val body: String,
    )
}
