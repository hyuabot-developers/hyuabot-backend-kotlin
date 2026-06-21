package app.hyuabot.backend.liveactivity.service

import app.hyuabot.backend.liveactivity.domain.ShuttleLiveActivityState
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.math.BigInteger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64

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
                .header("authorization", "bearer ${providerToken()}")
                .header("apns-topic", "$bundleId.push-type.liveactivity")
                .header("apns-push-type", "liveactivity")
                .header("apns-priority", "10")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
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

    private fun providerToken(): String {
        val header = base64Url(objectMapper.writeValueAsBytes(mapOf("alg" to "ES256", "kid" to keyId)))
        val claims = base64Url(objectMapper.writeValueAsBytes(mapOf("iss" to teamId, "iat" to Instant.now().epochSecond)))
        val signingInput = "$header.$claims"
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey())
        signature.update(signingInput.toByteArray(StandardCharsets.UTF_8))
        return "$signingInput.${base64Url(derToJose(signature.sign()))}"
    }

    private fun privateKey(): PrivateKey {
        val cleaned =
            privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\n", "")
                .replace("\n", "")
                .trim()
        val keyBytes = Base64.getDecoder().decode(cleaned)
        return KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(keyBytes))
    }

    private fun base64Url(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun derToJose(der: ByteArray): ByteArray {
        var index = 0
        require(der[index++].toInt() == 0x30)
        index = skipDerLength(der, index)
        require(der[index++].toInt() == 0x02)
        val rLength = der[index++].toInt() and 0xff
        val r = der.copyOfRange(index, index + rLength)
        index += rLength
        require(der[index++].toInt() == 0x02)
        val sLength = der[index++].toInt() and 0xff
        val s = der.copyOfRange(index, index + sLength)
        return fixedLength(r) + fixedLength(s)
    }

    private fun skipDerLength(
        der: ByteArray,
        index: Int,
    ): Int {
        val length = der[index].toInt() and 0xff
        if (length < 0x80) return index + 1
        return index + 1 + (length and 0x7f)
    }

    private fun fixedLength(value: ByteArray): ByteArray {
        val normalized = BigInteger(1, value).toByteArray().dropWhile { it == 0.toByte() }.toByteArray()
        return ByteArray(32 - normalized.size.coerceAtMost(32)) + normalized.takeLast(32).toByteArray()
    }
}
