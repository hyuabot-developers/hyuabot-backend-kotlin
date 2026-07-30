package app.hyuabot.backend.liveactivity.service

import app.hyuabot.backend.liveactivity.domain.ShuttleLiveActivityState
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant

@Service
class ApnsLiveActivityService(
    private val objectMapper: ObjectMapper,
    @param:Value("\${apns.live-activity.enabled:false}") private val enabled: Boolean,
    @param:Value("\${apns.live-activity.team-id:}") private val teamId: String,
    @param:Value("\${apns.live-activity.key-id:}") private val keyId: String,
    @param:Value("\${apns.live-activity.bundle-id:net.jaram.hyuabot}") private val bundleId: String,
    @param:Value("\${apns.live-activity.private-key:}") private val privateKeyPem: String,
    @param:Value("\${apns.live-activity.base-url:}") private val configuredBaseUrl: String,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build()
    private val signer: ApnsJwtSigner by lazy { ApnsJwtSigner(objectMapper, teamId, keyId, privateKeyPem) }

    fun sendUpdate(
        token: String,
        apnsEnvironment: String,
        state: ShuttleLiveActivityState,
        staleDate: Instant,
    ) {
        if (!isConfigured()) {
            logger.warn("APNs Live Activity push skipped because APNs is not configured.")
            return
        }

        val payload =
            mapOf(
                "aps" to
                    mapOf(
                        "timestamp" to Instant.now().epochSecond,
                        "event" to "update",
                        "stale-date" to staleDate.epochSecond,
                        "content-state" to state,
                    ),
            )
        send(token, apnsEnvironment, payload)
    }

    fun sendEnd(
        token: String,
        apnsEnvironment: String,
        state: ShuttleLiveActivityState,
    ) {
        if (!isConfigured()) {
            logger.warn("APNs Live Activity end skipped because APNs is not configured.")
            return
        }

        val payload =
            mapOf(
                "aps" to
                    mapOf(
                        "timestamp" to Instant.now().epochSecond,
                        "event" to "end",
                        "dismissal-date" to Instant.now().epochSecond,
                        "content-state" to state,
                    ),
            )
        send(token, apnsEnvironment, payload)
    }

    private fun send(
        token: String,
        apnsEnvironment: String,
        payload: Map<String, Any>,
    ) {
        val request =
            HttpRequest
                .newBuilder(URI.create("${baseUrl(apnsEnvironment)}/3/device/$token"))
                .header("authorization", "bearer ${signer.sign()}")
                .header("apns-topic", "$bundleId.push-type.liveactivity")
                .header("apns-push-type", "liveactivity")
                .header("apns-priority", "10")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (!isSuccessfulStatus(response.statusCode())) {
            logger.warn("APNs Live Activity push failed: status={} body={}", response.statusCode(), response.body())
        }
    }

    private fun isConfigured(): Boolean = enabled && teamId.isNotBlank() && keyId.isNotBlank() && privateKeyPem.isNotBlank()

    private fun baseUrl(apnsEnvironment: String): String =
        configuredBaseUrl.ifBlank {
            when (apnsEnvironment.lowercase()) {
                "development", "sandbox" -> "https://api.sandbox.push.apple.com"
                else -> "https://api.push.apple.com"
            }
        }

    private fun isSuccessfulStatus(statusCode: Int): Boolean = statusCode in 200..299
}
