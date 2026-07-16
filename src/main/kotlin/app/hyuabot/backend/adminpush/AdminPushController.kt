package app.hyuabot.backend.adminpush

import app.hyuabot.backend.adminpush.domain.AdminPushSubscriptionDeleteRequest
import app.hyuabot.backend.adminpush.domain.AdminPushSubscriptionRequest
import app.hyuabot.backend.adminpush.domain.AdminPushSubscriptionStatusResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClientException

@RestController
@RequestMapping("/api/v1/user/push")
class AdminPushController(
    private val notifierClient: NotifierClient,
) {
    @GetMapping("/public-key")
    fun getPublicKey(): ResponseEntity<*> = notifierResponse { ResponseEntity.ok(notifierClient.getPublicKey()) }

    @GetMapping("/status")
    fun getStatus(
        authentication: Authentication,
        @RequestParam endpoint: String,
    ): ResponseEntity<*> =
        notifierResponse {
            ResponseEntity.ok(notifierClient.getStatus(authentication.name, endpoint))
        }

    @PostMapping("/subscription")
    fun subscribe(
        authentication: Authentication,
        @RequestHeader("User-Agent", required = false) userAgent: String?,
        @RequestBody request: AdminPushSubscriptionRequest,
    ): ResponseEntity<*> =
        notifierResponse {
            notifierClient.subscribe(authentication.name, userAgent, request)
            ResponseEntity.noContent().build<Any>()
        }

    @DeleteMapping("/subscription")
    fun unsubscribe(
        authentication: Authentication,
        @RequestBody request: AdminPushSubscriptionDeleteRequest,
    ): ResponseEntity<*> =
        notifierResponse {
            notifierClient.unsubscribe(authentication.name, request.endpoint)
            ResponseEntity.noContent().build<Any>()
        }

    private fun notifierResponse(block: () -> ResponseEntity<*>): ResponseEntity<*> =
        try {
            block()
        } catch (_: RestClientException) {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                AdminPushSubscriptionStatusResponse(enabled = false),
            )
        }
}
