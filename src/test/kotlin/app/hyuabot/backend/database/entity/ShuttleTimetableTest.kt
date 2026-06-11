package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShuttleTimetableTest {
    private fun create(seq: Int? = 1) =
        ShuttleTimetable(
            seq = seq,
            periodType = "x",
            weekday = false,
            routeName = "x",
            departureTime = LocalTime.parse("09:00"),
            route = null,
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

    @Test
    fun mutators() {
        val e = create()
        e.periodType = "y"
        e.weekday = true
        e.routeName = "y"
        e.departureTime = LocalTime.parse("10:00")
    }
}
