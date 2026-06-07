package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalendarEventTest {
    private fun create(id: Int? = 1) =
        CalendarEvent(
            id = id,
            categoryID = 1,
            title = "x",
            description = "x",
            start = LocalDate.parse("2025-01-01"),
            end = LocalDate.parse("2025-01-01"),
            category = null,
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
}
