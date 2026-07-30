package app.hyuabot.backend.liveactivity.service

import tools.jackson.databind.ObjectMapper
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64

internal class ApnsJwtSigner(
    private val objectMapper: ObjectMapper,
    private val teamId: String,
    private val keyId: String,
    private val privateKeyPem: String,
) {
    fun sign(): String {
        val header = base64Url(objectMapper.writeValueAsBytes(mapOf("alg" to "ES256", "kid" to keyId)))
        val claims =
            base64Url(objectMapper.writeValueAsBytes(mapOf("iss" to teamId, "iat" to Instant.now().epochSecond)))
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

    internal fun derToJose(der: ByteArray): ByteArray {
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

    internal fun skipDerLength(
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
