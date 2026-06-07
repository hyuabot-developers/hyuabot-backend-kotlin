package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoomTest {
    private fun create(seq: Int? = 1) =
        Room(
            seq = seq,
            buildingName = "x",
            number = "x",
            name = "x",
            building = null,
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
        e.buildingName = "y"
        e.number = "y"
        e.name = "y"
    }
}
