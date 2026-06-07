package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BusRealtimeTest {
    private fun create(
        routeID: Int = 1,
        stopID: Int = 1,
        order: Int = 1,
    ) = BusRealtime(
        routeID = routeID,
        stopID = stopID,
        order = order,
        remainingStop = 1,
        remainingSeat = 1,
        remainingTime = Duration.ofMinutes(1),
        isLowFloor = false,
        updatedAt = ZonedDateTime.now(),
        routeStop = null,
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
        assertFalse(a == create(routeID = 9999))
        assertFalse(a == create(stopID = 9999))
        assertFalse(a == create(order = 9999))
    }

    @Test
    fun mutators() {
        val e = create()
        e.remainingStop = 2
        e.remainingSeat = 2
        e.remainingTime = Duration.ofMinutes(2)
        e.isLowFloor = true
        e.updatedAt = ZonedDateTime.now()
    }
}
