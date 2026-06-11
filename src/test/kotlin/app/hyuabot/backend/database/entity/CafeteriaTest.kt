package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CafeteriaTest {
    private fun create(id: Int = 1) =
        Cafeteria(
            id = id,
            campusID = 1,
            name = "x",
            latitude = 0.0,
            longitude = 0.0,
            breakfastTime = null,
            lunchTime = null,
            dinnerTime = null,
            campus = null,
            menu = mutableListOf(),
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
    }
}
