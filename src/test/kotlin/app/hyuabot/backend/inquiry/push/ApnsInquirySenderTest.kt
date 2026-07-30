package app.hyuabot.backend.inquiry.push

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.net.InetSocketAddress
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyPairGenerator
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApnsInquirySenderTest {
    @Test
    @DisplayName("Disabled inquiry sender skips network calls")
    fun disabledSender() {
        service(enabled = false).sendAlert("token", "thread", "title", "body")
    }

    @Test
    @DisplayName("Inquiry sender skips network calls when required credentials are blank")
    fun blankCredentialSender() {
        listOf(
            service(teamId = ""),
            service(keyId = ""),
            service(privateKeyPem = ""),
        ).forEach { it.sendAlert("token", "thread", "title", "body") }
    }

    @Test
    @DisplayName("Configured inquiry sender posts alert payload")
    fun configuredSender() {
        val received = mutableListOf<ReceivedRequest>()
        val server =
            HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
                createContext("/3/device/token") { exchange ->
                    received.add(
                        ReceivedRequest(
                            topic = exchange.requestHeaders.getFirst("apns-topic"),
                            pushType = exchange.requestHeaders.getFirst("apns-push-type"),
                            priority = exchange.requestHeaders.getFirst("apns-priority"),
                            authorization = exchange.requestHeaders.getFirst("authorization"),
                            body = exchange.requestBody.bufferedReader().readText(),
                        ),
                    )
                    exchange.sendResponseHeaders(200, 0)
                    exchange.responseBody.close()
                }
                start()
            }
        try {
            service(configuredBaseUrl = "http://127.0.0.1:${server.address.port}")
                .sendAlert("token", "thread-123", "title", "body")
            assertEquals(1, received.size)
            assertEquals("net.jaram.hyuabot", received[0].topic)
            assertEquals("alert", received[0].pushType)
            assertEquals("10", received[0].priority)
            assertTrue(received[0].authorization.startsWith("bearer "))
            assertTrue(received[0].body.contains("\"type\":\"inquiry\""))
            assertTrue(received[0].body.contains("\"threadId\":\"thread-123\""))
            assertTrue(received[0].body.contains("\"title\":\"title\""))
            assertTrue(received[0].body.contains("\"body\":\"body\""))
        } finally {
            server.stop(0)
        }
    }

    @Test
    @DisplayName("Inquiry sender handles failed responses")
    fun failedResponse() {
        val server =
            HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
                createContext("/3/device/token") { exchange ->
                    val bytes = "failed".toByteArray()
                    exchange.sendResponseHeaders(500, bytes.size.toLong())
                    exchange.responseBody.write(bytes)
                    exchange.responseBody.close()
                }
                start()
            }
        try {
            service(configuredBaseUrl = "http://127.0.0.1:${server.address.port}")
                .sendAlert("token", "thread", "title", "body")
        } finally {
            server.stop(0)
        }
    }

    @Test
    @DisplayName("Inquiry sender handles HTTP send exceptions")
    fun httpExceptionHandled() {
        val sender =
            object : ApnsInquirySender(
                objectMapper = JsonMapper.builder().build(),
                enabled = true,
                teamId = "TEAMID",
                keyId = "KEYID",
                bundleId = "net.jaram.hyuabot",
                privateKeyPem = privateKeyPem(),
                configuredBaseUrl = "http://127.0.0.1:1",
            ) {
                override fun executeRequest(request: HttpRequest): HttpResponse<String>? = null
            }
        sender.sendAlert("token", "thread", "title", "body")
    }

    @Test
    @DisplayName("Default base URL is used when configured value is blank")
    fun defaultBaseUrl() {
        val service = service(configuredBaseUrl = "")
        val method = ApnsInquirySender::class.java.getDeclaredMethod("baseUrl")
        method.isAccessible = true
        assertEquals("https://api.push.apple.com", method.invoke(service))
    }

    @Test
    @DisplayName("Success status code range is bounded")
    fun successRange() {
        val service = service()
        val method =
            ApnsInquirySender::class.java.getDeclaredMethod("isSuccessfulStatus", Int::class.javaPrimitiveType)
        method.isAccessible = true
        assertEquals(false, method.invoke(service, 199))
        assertEquals(true, method.invoke(service, 200))
        assertEquals(true, method.invoke(service, 299))
        assertEquals(false, method.invoke(service, 300))
    }

    @Test
    @DisplayName("Real executeRequest propagates network failure as null")
    fun executeRequestReturnsNullOnException() {
        val service = service(configuredBaseUrl = "http://127.0.0.1:1")
        val method =
            ApnsInquirySender::class.java.getDeclaredMethod("executeRequest", HttpRequest::class.java)
        method.isAccessible = true
        val request =
            HttpRequest
                .newBuilder(java.net.URI.create("http://127.0.0.1:1/3/device/token"))
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build()
        assertEquals(null, method.invoke(service, request))
    }

    private fun service(
        enabled: Boolean = true,
        teamId: String = "TEAMID",
        keyId: String = "KEYID",
        privateKeyPem: String = privateKeyPem(),
        configuredBaseUrl: String = "http://127.0.0.1:1",
    ): ApnsInquirySender =
        ApnsInquirySender(
            objectMapper = JsonMapper.builder().build(),
            enabled = enabled,
            teamId = teamId,
            keyId = keyId,
            bundleId = "net.jaram.hyuabot",
            privateKeyPem = privateKeyPem,
            configuredBaseUrl = configuredBaseUrl,
        )

    private fun privateKeyPem(): String {
        val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        return Base64.getEncoder().encodeToString(keyPair.private.encoded)
    }

    private data class ReceivedRequest(
        val topic: String,
        val pushType: String,
        val priority: String,
        val authorization: String,
        val body: String,
    )
}
