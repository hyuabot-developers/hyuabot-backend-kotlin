package app.hyuabot.backend.adminpush.domain

data class AdminPushSubscriptionRequest(
    val endpoint: String,
    val keys: AdminPushSubscriptionKeys,
)

data class AdminPushSubscriptionKeys(
    val p256dh: String,
    val auth: String,
)

data class AdminPushSubscriptionDeleteRequest(
    val endpoint: String,
)

data class AdminPushPublicKeyResponse(
    val publicKey: String,
)

data class AdminPushSubscriptionStatusResponse(
    val enabled: Boolean,
)

internal data class NotifierSubscriptionRequest(
    val userId: String,
    val endpoint: String,
    val keys: AdminPushSubscriptionKeys,
    val userAgent: String?,
)

internal data class NotifierSubscriptionDeleteRequest(
    val userId: String,
    val endpoint: String,
)
