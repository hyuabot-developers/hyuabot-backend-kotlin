package app.hyuabot.backend.config

import app.hyuabot.backend.inquiry.push.FirebaseMessagingWrapper
import app.hyuabot.backend.inquiry.push.NoopFirebaseMessagingWrapper
import app.hyuabot.backend.inquiry.push.RealFirebaseMessagingWrapper
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream

@Configuration
class FirebaseConfig(
    @param:Value("\${firebase.enabled:false}") private val enabled: Boolean,
    @param:Value("\${firebase.credentials-path:}") private val credentialsPath: String,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun firebaseMessagingWrapper(): FirebaseMessagingWrapper {
        if (!enabled || credentialsPath.isBlank()) {
            logger.info("Firebase disabled; using noop FCM wrapper")
            return NoopFirebaseMessagingWrapper()
        }
        return try {
            val credentials = FileInputStream(credentialsPath).use { GoogleCredentials.fromStream(it) }
            val options = FirebaseOptions.builder().setCredentials(credentials).build()
            val app = if (FirebaseApp.getApps().isEmpty()) FirebaseApp.initializeApp(options, "inquiry") else FirebaseApp.getApps().first()
            RealFirebaseMessagingWrapper(FirebaseMessaging.getInstance(app))
        } catch (e: Exception) {
            logger.error("Failed to initialize Firebase; falling back to noop wrapper", e)
            NoopFirebaseMessagingWrapper()
        }
    }
}
