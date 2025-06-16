package app.hyuabot.backend.config

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class JasyptConfigTest {
    @Test
    @DisplayName("Test method that provides Jasypt encryptor")
    fun provideJasyptEncryptor() {
        val jasyptConfig = JasyptConfig("testPassword")
        val encryptor = jasyptConfig.provideJasyptEncryptor()

        val encrypted = encryptor.encrypt("testString")
        val decrypted = encryptor.decrypt(encrypted)
        assert(decrypted == "testString")
    }
}
