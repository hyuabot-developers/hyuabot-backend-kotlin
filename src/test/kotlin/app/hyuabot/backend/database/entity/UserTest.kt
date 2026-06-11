package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserTest {
    private fun create(userID: String = "x") =
        User(
            userID = userID,
            password = ByteArray(0),
            name = "x",
            email = "x",
            phone = "x",
            active = false,
            refreshToken = mutableListOf(),
            notice = mutableListOf(),
        )

    @Test
    fun equalsAndHashCode() {
        val a = create()
        val nullValue: Any? = null
        assertTrue(a == a)
        assertFalse(a.equals(nullValue))
        assertFalse(a.equals(Any()))
        assertEquals(a, create())
        assertEquals(a.hashCode(), create().hashCode())
        assertFalse(a == create(userID = "different"))
    }

    @Test
    fun mutators() {
        val e = create()
        e.password = ByteArray(1)
        e.name = "y"
        e.email = "y"
        e.phone = "y"
        e.active = true
    }
}
