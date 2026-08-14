package app.hyuabot.backend.inquiry.push

import app.hyuabot.backend.adminpush.NotifierClient
import app.hyuabot.backend.adminpush.domain.NotifierInquiryNotification
import app.hyuabot.backend.database.entity.DevicePushToken
import app.hyuabot.backend.database.repository.DevicePushTokenRepository
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class InquiryPushService(
    private val tokenRepository: DevicePushTokenRepository,
    private val apnsSender: ApnsInquirySender,
    private val fcmWrapper: FirebaseMessagingWrapper,
    private val notifierClient: NotifierClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private fun now(): ZonedDateTime = ZonedDateTime.now(LocalDateTimeBuilder.serviceTimezone)

    @Transactional
    fun registerToken(
        installationId: UUID,
        provider: String,
        token: String,
        platform: String,
    ) {
        val timestamp = now()
        val existing = tokenRepository.findByProviderAndToken(provider, token)
        if (existing != null) {
            existing.installationId = installationId
            existing.platform = platform
            existing.updatedAt = timestamp
            tokenRepository.save(existing)
            return
        }
        tokenRepository.save(
            DevicePushToken(
                installationId = installationId,
                platform = platform,
                provider = provider,
                token = token,
                createdAt = timestamp,
                updatedAt = timestamp,
            ),
        )
    }

    @Transactional
    fun unregisterToken(
        provider: String,
        token: String,
    ) {
        tokenRepository.deleteByProviderAndToken(provider, token)
    }

    fun notifyAdminMessage(
        installationId: UUID,
        threadId: UUID,
        title: String,
        preview: String,
    ) {
        val tokens = tokenRepository.findByInstallationId(installationId)
        if (tokens.isEmpty()) return
        tokens.forEach { record ->
            try {
                when (record.provider) {
                    "APNS" -> apnsSender.sendAlert(record.token, threadId.toString(), title, preview)
                    "FCM" ->
                        fcmWrapper.send(
                            record.token,
                            title,
                            preview,
                            mapOf("type" to "inquiry", "threadId" to threadId.toString()),
                        )
                    else -> logger.warn("Unknown push provider: {}", record.provider)
                }
            } catch (e: Exception) {
                logger.warn("Failed to send inquiry push (provider={} token={}): {}", record.provider, record.token, e.message)
            }
        }
    }

    fun notifyAdminInquiry(
        threadId: UUID,
        body: String,
    ) {
        try {
            notifierClient.notifyInquiry(
                NotifierInquiryNotification(
                    title = "새 문의가 도착했어요",
                    body = body.take(100),
                    url = "/inquiry?threadId=$threadId",
                    tag = "inquiry:$threadId",
                ),
            )
        } catch (e: Exception) {
            logger.warn("Failed to send admin inquiry push: {}", e.message)
        }
    }
}
