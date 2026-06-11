package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BusRouteStopTest {
    private fun create(seq: Int? = 1) =
        BusRouteStop(
            seq = seq,
            routeID = 1,
            stopID = 1,
            order = 1,
            startStopID = 1,
            minuteFromStart = 1,
            route = null,
            stop = null,
            startStop = null,
            log = mutableListOf(),
            realtime = mutableListOf(),
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
        e.routeID = 2
        e.stopID = 2
        e.order = 2
        e.startStopID = 2
        e.minuteFromStart = 2
    }
}
