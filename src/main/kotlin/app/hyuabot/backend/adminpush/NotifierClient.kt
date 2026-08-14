package app.hyuabot.backend.adminpush

import app.hyuabot.backend.adminpush.domain.AdminPushPublicKeyResponse
import app.hyuabot.backend.adminpush.domain.AdminPushSubscriptionRequest
import app.hyuabot.backend.adminpush.domain.AdminPushSubscriptionStatusResponse
import app.hyuabot.backend.adminpush.domain.NotifierInquiryNotification
import app.hyuabot.backend.adminpush.domain.NotifierSubscriptionDeleteRequest
import app.hyuabot.backend.adminpush.domain.NotifierSubscriptionRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class NotifierClient(
    @param:Value("\${admin.push.notifier-base-url:http://127.0.0.1:8081}") baseURL: String,
    @param:Value("\${admin.push.notifier-service-token:}") serviceToken: String,
) {
    private val restClient =
        RestClient
            .builder()
            .baseUrl(baseURL)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $serviceToken")
            .build()

    fun getPublicKey(): AdminPushPublicKeyResponse =
        requireNotNull(
            restClient
                .get()
                .uri("/api/v1/push/public-key")
                .retrieve()
                .body(AdminPushPublicKeyResponse::class.java),
        ) { "Notifier returned an empty public key response" }

    fun getStatus(
        userID: String,
        endpoint: String,
    ): AdminPushSubscriptionStatusResponse =
        requireNotNull(
            restClient
                .get()
                .uri { builder ->
                    builder
                        .path("/api/v1/push/subscriptions/status")
                        .queryParam("userId", userID)
                        .queryParam("endpoint", endpoint)
                        .build()
                }.retrieve()
                .body(AdminPushSubscriptionStatusResponse::class.java),
        ) { "Notifier returned an empty subscription status response" }

    fun subscribe(
        userID: String,
        userAgent: String?,
        request: AdminPushSubscriptionRequest,
    ) {
        restClient
            .post()
            .uri("/api/v1/push/subscriptions")
            .body(NotifierSubscriptionRequest(userID, request.endpoint, request.keys, userAgent))
            .retrieve()
            .toBodilessEntity()
    }

    fun unsubscribe(
        userID: String,
        endpoint: String,
    ) {
        restClient
            .method(HttpMethod.DELETE)
            .uri("/api/v1/push/subscriptions")
            .body(NotifierSubscriptionDeleteRequest(userID, endpoint))
            .retrieve()
            .toBodilessEntity()
    }

    fun notifyInquiry(notification: NotifierInquiryNotification) {
        restClient
            .post()
            .uri("/api/v1/push/inquiry")
            .body(notification)
            .retrieve()
            .toBodilessEntity()
    }
}
