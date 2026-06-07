package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MenuTest {
    private fun create(seq: Int? = 1) =
        Menu(
            seq = seq,
            restaurantID = 1,
            date = LocalDate.parse("2025-01-01"),
            type = "x",
            food = "x",
            price = "x",
            cafeteria = null,
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
