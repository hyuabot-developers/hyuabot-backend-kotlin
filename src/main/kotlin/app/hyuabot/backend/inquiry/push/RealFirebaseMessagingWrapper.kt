package app.hyuabot.backend.inquiry.push

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification

class RealFirebaseMessagingWrapper(
    private val messaging: FirebaseMessaging,
) : FirebaseMessagingWrapper {
    override fun send(
        deviceToken: String,
        title: String,
        body: String,
        data: Map<String, String>,
    ): String? {
        val message =
            Message
                .builder()
                .setToken(deviceToken)
                .setNotification(
                    Notification
                        .builder()
                        .setTitle(title)
                        .setBody(body)
                        .build(),
                ).putAllData(data)
                .build()
        return messaging.send(message)
    }
}
