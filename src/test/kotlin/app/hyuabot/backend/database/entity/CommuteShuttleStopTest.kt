package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommuteShuttleStopTest {
    private fun create(name: String = "x") =
        CommuteShuttleStop(
            name = name,
            description = "x",
            latitude = 0.0,
            longitude = 0.0,
            timetable = mutableListOf(),
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
        assertFalse(a == create(name = "y"))
    }
}
