package app.hyuabot.backend.liveactivity

import app.hyuabot.backend.liveactivity.domain.ShuttleLiveActivityState
import app.hyuabot.backend.liveactivity.service.ApnsLiveActivityService
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.net.InetSocketAddress
import java.security.KeyPairGenerator
import java.time.Instant
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApnsLiveActivityServiceTest {
    @Test
    @DisplayName("Disabled APNs sender skips network calls")
    fun disabledSender() {
        val service =
            ApnsLiveActivityService(
                objectMapper = JsonMapper.builder().build(),
                enabled = false,
                teamId = "",
                keyId = "",
                bundleId = "net.jaram.hyuabot",
                privateKeyPem = "",
                configuredBaseUrl = "http://127.0.0.1:1",
            )

        service.sendUpdate("token", "development", state(), Instant.parse("2026-06-21T00:10:00Z"))
        service.sendEnd("token", "development", state())
    }

    @Test
    @DisplayName("Configured APNs sender posts Live Activity update and end payloads")
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
            val service = configuredService("http://127.0.0.1:${server.address.port}")

            service.sendUpdate("token", "development", state(), Instant.parse("2026-06-21T00:10:00Z"))
            service.sendEnd("token", "production", state())

            assertEquals(2, received.size)
            assertTrue(received.all { it.topic == "net.jaram.hyuabot.push-type.liveactivity" })
            assertTrue(received.all { it.pushType == "liveactivity" })
            assertTrue(received.all { it.priority == "10" })
            assertTrue(received.all { it.authorization.startsWith("bearer ") })
            assertTrue(received[0].body.contains("\"event\":\"update\""))
            assertTrue(received[0].body.contains("\"stale-date\":1782000600"))
            assertTrue(received[1].body.contains("\"event\":\"end\""))
            assertTrue(received[1].body.contains("\"dismissal-date\""))
        } finally {
            server.stop(0)
        }
    }

    @Test
    @DisplayName("APNs sender handles failed responses")
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
            configuredService("http://127.0.0.1:${server.address.port}")
                .sendUpdate("token", "development", state(), Instant.parse("2026-06-21T00:10:00Z"))
        } finally {
            server.stop(0)
        }
    }

    @Test
    @DisplayName("DER long length parser skips extended length bytes")
    fun derLongLengthParser() {
        val service = configuredService("http://127.0.0.1:1")
        val method =
            ApnsLiveActivityService::class.java.getDeclaredMethod(
                "skipDerLength",
                ByteArray::class.java,
                Int::class.javaPrimitiveType,
            )
        method.isAccessible = true

        assertEquals(3, method.invoke(service, byteArrayOf(0, 0x81.toByte(), 0), 1))
    }

    @Test
    @DisplayName("APNs endpoint is selected from token environment")
    fun endpointFromEnvironment() {
        val service = configuredService("")
        val method =
            ApnsLiveActivityService::class.java.getDeclaredMethod(
                "baseUrl",
                String::class.java,
            )
        method.isAccessible = true

        assertEquals("https://api.sandbox.push.apple.com", method.invoke(service, "development"))
        assertEquals("https://api.sandbox.push.apple.com", method.invoke(service, "sandbox"))
        assertEquals("https://api.push.apple.com", method.invoke(service, "production"))
        assertEquals("https://api.push.apple.com", method.invoke(service, "unknown"))
    }

    private fun configuredService(baseUrl: String): ApnsLiveActivityService =
        ApnsLiveActivityService(
            objectMapper = JsonMapper.builder().build(),
            enabled = true,
            teamId = "TEAMID",
            keyId = "KEYID",
            bundleId = "net.jaram.hyuabot",
            privateKeyPem = privateKeyPem(),
            configuredBaseUrl = baseUrl,
        )

    private fun privateKeyPem(): String {
        val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        return Base64.getEncoder().encodeToString(keyPair.private.encoded)
    }

    private fun state() =
        ShuttleLiveActivityState(
            titleText = "title",
            statusText = "status",
            dynamicIslandStatusText = "island",
            currentStopName = "current",
            nextStopName = "next",
            checkpointStopNames = listOf("current", "next"),
            checkpointTimes = listOf(1.0, 2.0),
            checkpointWaitingFormat = "%s waiting",
            checkpointApproachingFormat = "%s approaching",
            checkpointDepartedFormat = "%s departed",
            progress = 50,
            progressSegments = listOf(100),
        )

    private data class ReceivedRequest(
        val topic: String,
        val pushType: String,
        val priority: String,
        val authorization: String,
        val body: String,
    )
}
