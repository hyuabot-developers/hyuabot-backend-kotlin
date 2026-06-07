package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuildingTest {
    private fun create(name: String = "x") =
        Building(
            id = null,
            name = name,
            campusID = 1,
            latitude = 0.0,
            longitude = 0.0,
            url = null,
            room = mutableListOf(),
            campus = null,
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
