package app.hyuabot.backend.inquiry.push

import app.hyuabot.backend.liveactivity.service.ApnsJwtSigner
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Service
open class ApnsInquirySender(
    private val objectMapper: ObjectMapper,
    @param:Value("\${apns.inquiry.enabled:false}") private val enabled: Boolean,
    @param:Value("\${apns.inquiry.team-id:}") private val teamId: String,
    @param:Value("\${apns.inquiry.key-id:}") private val keyId: String,
    @param:Value("\${apns.inquiry.bundle-id:net.jaram.hyuabot}") private val bundleId: String,
    @param:Value("\${apns.inquiry.private-key:}") private val privateKeyPem: String,
    @param:Value("\${apns.inquiry.base-url:}") private val configuredBaseUrl: String,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val client: HttpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build()
    private val signer: ApnsJwtSigner by lazy { ApnsJwtSigner(objectMapper, teamId, keyId, privateKeyPem) }

    fun sendAlert(
        deviceToken: String,
        threadId: String,
        title: String,
        body: String,
    ) {
        if (!isConfigured()) {
            logger.warn("APNs inquiry push skipped because APNs is not configured.")
            return
        }
        val payload =
            mapOf(
                "aps" to
                    mapOf(
                        "alert" to mapOf("title" to title, "body" to body),
                        "sound" to "default",
                    ),
                "type" to "inquiry",
                "threadId" to threadId,
            )
        val request =
            HttpRequest
                .newBuilder(URI.create("${baseUrl()}/3/device/$deviceToken"))
                .header("authorization", "bearer ${signer.sign()}")
                .header("apns-topic", bundleId)
                .header("apns-push-type", "alert")
                .header("apns-priority", "10")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build()
        val response = executeRequest(request)
        if (response == null || !isSuccessfulStatus(response.statusCode())) {
            logger.warn(
                "APNs inquiry push failed: status={} body={}",
                response?.statusCode(),
                response?.body(),
            )
        }
    }

    open fun executeRequest(request: HttpRequest): HttpResponse<String>? =
        try {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            logger.warn("APNs inquiry HTTP send failed", e)
            null
        }

    private fun isSuccessfulStatus(statusCode: Int): Boolean = statusCode in 200..299

    private fun isConfigured(): Boolean = enabled && teamId.isNotBlank() && keyId.isNotBlank() && privateKeyPem.isNotBlank()

    private fun baseUrl(): String = configuredBaseUrl.ifBlank { "https://api.push.apple.com" }
}
