package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoticeTest {
    private fun create(id: Int? = 1) =
        Notice(
            id = id,
            title = "x",
            url = "x",
            expiredAt = ZonedDateTime.now(),
            categoryID = 1,
            userID = "x",
            language = "x",
            category = null,
            user = null,
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
        assertFalse(a == create(id = 9999))
        assertFalse(create(id = null) == create(id = null))
    }

    @Test
    fun mutators() {
        val e = create()
        e.title = "y"
        e.url = "y"
        e.expiredAt = ZonedDateTime.now()
        e.categoryID = 2
        e.userID = "y"
        e.language = "y"
    }
}
