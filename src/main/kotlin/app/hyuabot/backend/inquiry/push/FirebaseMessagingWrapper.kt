package app.hyuabot.backend.inquiry.push

interface FirebaseMessagingWrapper {
    /**
     * Send an FCM push message.
     * @return message ID on success, or null if the wrapper is a no-op.
     */
    fun send(
        deviceToken: String,
        title: String,
        body: String,
        data: Map<String, String>,
    ): String?
}
