package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommuteShuttleTimetableTest {
    private fun create(seq: Int? = 1) =
        CommuteShuttleTimetable(
            seq = seq,
            routeName = "x",
            stopName = "x",
            order = 1,
            departureTime = LocalTime.parse("09:00"),
            route = null,
            stop = null,
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
