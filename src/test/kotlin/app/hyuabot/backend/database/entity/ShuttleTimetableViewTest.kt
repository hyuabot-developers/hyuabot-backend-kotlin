package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShuttleTimetableViewTest {
    private fun create(
        seq: Int = 1,
        stopName: String = "x",
        destinationGroup: String = "x",
    ) = ShuttleTimetableView(
        seq = seq,
        periodType = "x",
        weekday = false,
        routeName = "x",
        routeTag = "x",
        stopName = stopName,
        departureTime = LocalTime.parse("09:00"),
        destinationGroup = destinationGroup,
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
        assertFalse(a == create(stopName = "y"))
        assertFalse(a == create(destinationGroup = "y"))
    }
}
