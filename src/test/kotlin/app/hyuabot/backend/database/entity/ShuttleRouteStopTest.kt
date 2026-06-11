package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShuttleRouteStopTest {
    private fun create(seq: Int? = 1) =
        ShuttleRouteStop(
            seq = seq,
            routeName = "x",
            stopName = "x",
            order = 1,
            cumulativeTime = Duration.ofMinutes(1),
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

    @Test
    fun mutators() {
        val e = create()
        e.routeName = "y"
        e.stopName = "y"
        e.order = 2
        e.cumulativeTime = Duration.ofMinutes(2)
    }
}
