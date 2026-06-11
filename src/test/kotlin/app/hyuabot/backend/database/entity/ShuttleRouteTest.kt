package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShuttleRouteTest {
    private fun create(name: String = "x") =
        ShuttleRoute(
            name = name,
            descriptionKorean = "x",
            descriptionEnglish = "x",
            tag = "x",
            startStopID = "x",
            endStopID = "x",
            timetable = mutableListOf(),
            stop = mutableListOf(),
            startStop = null,
            endStop = null,
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
        assertFalse(a == create(name = "different"))
    }
}
