package app.hyuabot.backend.inquiry.push

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FirebaseMessagingWrapperTest {
    @Test
    fun noopReturnsNull() {
        val wrapper = NoopFirebaseMessagingWrapper()
        assertNull(wrapper.send("token", "title", "body", mapOf("k" to "v")))
    }

    @Test
    fun realDelegatesToFirebaseMessaging() {
        val messaging: FirebaseMessaging = mock()
        whenever(messaging.send(any<Message>())).thenReturn("message-id")
        val wrapper = RealFirebaseMessagingWrapper(messaging)
        val result = wrapper.send("device", "title", "body", mapOf("type" to "inquiry"))
        assertEquals("message-id", result)
        verify(messaging).send(any<Message>())
    }
}
