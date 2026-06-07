package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadingRoomTest {
    private fun create(id: Int = 1) =
        ReadingRoom(
            id = id,
            name = "x",
            campusID = 1,
            isActive = false,
            isReservable = false,
            total = 1,
            active = 1,
            occupied = 1,
            updatedAt = ZonedDateTime.now(),
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
        assertFalse(a == create(id = 9999))
    }

    @Test
    fun mutators() {
        val e = create()
        e.name = "y"
        e.campusID = 2
        e.isActive = true
        e.isReservable = true
        e.total = 2
        e.active = 2
        e.occupied = 2
        e.available = 2
        e.updatedAt = ZonedDateTime.now()
    }
}
