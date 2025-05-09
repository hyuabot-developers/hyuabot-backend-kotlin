package app.hyuabot.backend.config

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JasyptConfig(
    @Value("\${jasypt.encryptor.password}") private val jasyptPassword: String,
) {
    @Bean("jasyptStringEncryptor")
    fun provideJasyptEncryptor(): StandardPBEStringEncryptor =
        StandardPBEStringEncryptor().apply {
            setPassword(jasyptPassword)
            setAlgorithm("PBEWithMD5AndDES")
            setKeyObtentionIterations(1000)
        }
}
