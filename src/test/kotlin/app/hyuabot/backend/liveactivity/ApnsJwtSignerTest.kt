package app.hyuabot.backend.liveactivity

import app.hyuabot.backend.liveactivity.service.ApnsJwtSigner
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.security.KeyPairGenerator
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ApnsJwtSignerTest {
    private fun signerWithGeneratedKey(): ApnsJwtSigner {
        val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        val pemKey = Base64.getEncoder().encodeToString(keyPair.private.encoded)
        return ApnsJwtSigner(
            objectMapper = JsonMapper.builder().build(),
            teamId = "TEAMID",
            keyId = "KEYID",
            privateKeyPem = pemKey,
        )
    }

    @Test
    @DisplayName("sign() returns a three-part JWT string")
    fun signReturnsJwt() {
        val token = signerWithGeneratedKey().sign()
        val parts = token.split(".")
        assertEquals(3, parts.size)
        assertTrue(parts.all { it.isNotBlank() })
    }

    @Test
    @DisplayName("skipDerLength - short-form length (< 0x80) advances index by 1")
    fun skipDerLengthShortForm() {
        val signer = signerWithGeneratedKey()
        // length byte = 0x20 (< 0x80) → return index + 1
        val result = signer.skipDerLength(byteArrayOf(0x20), 0)
        assertEquals(1, result)
    }

    @Test
    @DisplayName("skipDerLength - long-form length advances index by 1 + len-of-len bytes")
    fun skipDerLengthLongForm() {
        val signer = signerWithGeneratedKey()
        // byte[0]=0, byte[1]=0x81 (long-form, 1 additional byte), byte[2]=0
        // at index=1: length=0x81 (≥0x80), lengthOfLen = 0x81 & 0x7f = 1 → return 1 + 1 + 1 = 3
        val result = signer.skipDerLength(byteArrayOf(0, 0x81.toByte(), 0), 1)
        assertEquals(3, result)
    }

    @Test
    @DisplayName("derToJose - rejects invalid sequence marker (0x31 instead of 0x30)")
    fun derToJoseRejectsInvalidSequenceMarker() {
        val signer = signerWithGeneratedKey()
        assertFailsWith<IllegalArgumentException> {
            signer.derToJose(byteArrayOf(0x31, 0))
        }
    }

    @Test
    @DisplayName("derToJose - rejects invalid first integer marker (0x03 instead of 0x02)")
    fun derToJoseRejectsInvalidFirstIntegerMarker() {
        val signer = signerWithGeneratedKey()
        assertFailsWith<IllegalArgumentException> {
            signer.derToJose(byteArrayOf(0x30, 0x06, 0x03))
        }
    }

    @Test
    @DisplayName("derToJose - rejects invalid second integer marker (0x03 instead of 0x02)")
    fun derToJoseRejectsInvalidSecondIntegerMarker() {
        val signer = signerWithGeneratedKey()
        assertFailsWith<IllegalArgumentException> {
            signer.derToJose(byteArrayOf(0x30, 0x06, 0x02, 0x01, 0x01, 0x03))
        }
    }

    @Test
    @DisplayName("sign() works with PEM-style key (with header/footer and newlines)")
    fun signWithPemStyleKey() {
        val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        val rawBase64 = Base64.getEncoder().encodeToString(keyPair.private.encoded)
        // Wrap with PEM header/footer and \n escape sequences
        val pemKey = "-----BEGIN PRIVATE KEY-----\\n${rawBase64}\\n-----END PRIVATE KEY-----"
        val signer =
            ApnsJwtSigner(
                objectMapper = JsonMapper.builder().build(),
                teamId = "T",
                keyId = "K",
                privateKeyPem = pemKey,
            )
        val token = signer.sign()
        assertEquals(3, token.split(".").size)
    }
}
