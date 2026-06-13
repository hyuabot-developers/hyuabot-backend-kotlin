package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PublicHolidayTest {
    private fun create(seq: Int? = 1) =
        PublicHoliday(
            seq = seq,
            date = LocalDate.of(2025, 1, 1),
            name = "신정",
            calendarType = "solar",
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
        assertFalse(a == create(seq = 9999))
        assertFalse(create(seq = null) == create(seq = null))
    }
}
