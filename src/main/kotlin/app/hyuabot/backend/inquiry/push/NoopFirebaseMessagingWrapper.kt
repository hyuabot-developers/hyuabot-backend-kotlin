package app.hyuabot.backend.inquiry.push

class NoopFirebaseMessagingWrapper : FirebaseMessagingWrapper {
    override fun send(
        deviceToken: String,
        title: String,
        body: String,
        data: Map<String, String>,
    ): String? = null
}
