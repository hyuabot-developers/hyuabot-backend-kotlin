package app.hyuabot.backend.inquiry.push

import app.hyuabot.backend.adminpush.NotifierClient
import app.hyuabot.backend.database.entity.DevicePushToken
import app.hyuabot.backend.database.repository.DevicePushTokenRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class InquiryPushServiceTest {
    @Mock lateinit var tokenRepository: DevicePushTokenRepository

    @Mock lateinit var apnsSender: ApnsInquirySender

    @Mock lateinit var fcmWrapper: FirebaseMessagingWrapper

    @Mock lateinit var notifierClient: NotifierClient

    @InjectMocks lateinit var service: InquiryPushService

    private val installationId: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val threadId: UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")

    @Test
    @DisplayName("registerToken saves new record when none exists")
    fun registerNew() {
        whenever(tokenRepository.findByProviderAndToken("APNS", "tok")).thenReturn(null)
        whenever(tokenRepository.save(any<DevicePushToken>())).thenAnswer { it.arguments[0] }
        service.registerToken(installationId, "APNS", "tok", "IOS")
        verify(tokenRepository).save(
            argThat<DevicePushToken> {
                installationId == this@InquiryPushServiceTest.installationId &&
                    provider == "APNS" &&
                    token == "tok" &&
                    platform == "IOS"
            },
        )
    }

    @Test
    @DisplayName("registerToken updates existing record")
    fun registerExisting() {
        val existing =
            DevicePushToken(
                id = 1L,
                installationId = UUID.fromString("00000000-0000-0000-0000-000000000000"),
                platform = "ANDROID",
                provider = "FCM",
                token = "tok",
                createdAt = ZonedDateTime.now(),
                updatedAt = ZonedDateTime.now(),
            )
        whenever(tokenRepository.findByProviderAndToken("FCM", "tok")).thenReturn(existing)
        whenever(tokenRepository.save(existing)).thenReturn(existing)
        service.registerToken(installationId, "FCM", "tok", "ANDROID")
        assertEquals(installationId, existing.installationId)
        assertEquals("ANDROID", existing.platform)
        verify(tokenRepository).save(existing)
    }

    @Test
    @DisplayName("unregisterToken delegates to repository")
    fun unregister() {
        service.unregisterToken("APNS", "tok")
        verify(tokenRepository).deleteByProviderAndToken("APNS", "tok")
    }

    @Test
    @DisplayName("notifyAdminMessage does nothing when no tokens")
    fun notifyNone() {
        whenever(tokenRepository.findByInstallationId(installationId)).thenReturn(emptyList())
        service.notifyAdminMessage(installationId, threadId, "title", "body")
        verify(apnsSender, never()).sendAlert(any(), any(), any(), any())
        verify(fcmWrapper, never()).send(any(), any(), any(), any())
    }

    @Test
    @DisplayName("notifyAdminMessage dispatches by provider")
    fun notifyDispatch() {
        val tokens =
            listOf(
                token(1L, "APNS", "apns-token"),
                token(2L, "FCM", "fcm-token"),
                token(3L, "OTHER", "other-token"),
            )
        whenever(tokenRepository.findByInstallationId(installationId)).thenReturn(tokens)
        service.notifyAdminMessage(installationId, threadId, "title", "preview")
        verify(apnsSender).sendAlert("apns-token", threadId.toString(), "title", "preview")
        verify(fcmWrapper).send(
            eq("fcm-token"),
            eq("title"),
            eq("preview"),
            argThat { this["type"] == "inquiry" && this["threadId"] == threadId.toString() },
        )
    }

    @Test
    @DisplayName("notifyAdminMessage swallows sender exceptions")
    fun notifySwallowException() {
        val tokens = listOf(token(1L, "APNS", "apns-token"), token(2L, "FCM", "fcm-token"))
        whenever(tokenRepository.findByInstallationId(installationId)).thenReturn(tokens)
        whenever(apnsSender.sendAlert(any(), any(), any(), any())).thenThrow(RuntimeException("apns-fail"))
        whenever(fcmWrapper.send(any(), any(), any(), any())).thenThrow(RuntimeException("fcm-fail"))
        service.notifyAdminMessage(installationId, threadId, "t", "b")
    }

    @Test
    @DisplayName("notifyAdminInquiry sends a web push notification")
    fun notifyAdminInquiry() {
        service.notifyAdminInquiry(threadId, "문의 내용")
        verify(notifierClient).notifyInquiry(
            argThat {
                title == "새 문의가 도착했어요" &&
                    body == "문의 내용" &&
                    url == "/inquiry?threadId=$threadId" &&
                    tag == "inquiry:$threadId"
            },
        )
    }

    @Test
    @DisplayName("notifyAdminInquiry swallows notifier exceptions")
    fun notifyAdminInquirySwallowException() {
        whenever(notifierClient.notifyInquiry(any())).thenThrow(RuntimeException("push-fail"))
        service.notifyAdminInquiry(threadId, "문의 내용")
    }

    private fun token(
        id: Long,
        provider: String,
        token: String,
    ) = DevicePushToken(
        id = id,
        installationId = installationId,
        platform = if (provider == "APNS") "IOS" else "ANDROID",
        provider = provider,
        token = token,
        createdAt = ZonedDateTime.now(),
        updatedAt = ZonedDateTime.now(),
    )
}
