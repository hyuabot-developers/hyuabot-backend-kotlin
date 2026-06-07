package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContactTest {
    private fun create(id: Int? = 1) =
        Contact(
            id = id,
            campusID = 1,
            categoryID = 1,
            name = "x",
            phone = "x",
            category = null,
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
        assertFalse(create(id = null) == create(id = null))
    }
}
