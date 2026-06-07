package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BusDepartureLogTest {
    private fun create(seq: Int? = 1) =
        BusDepartureLog(
            seq = seq,
            routeID = 1,
            stopID = 1,
            departureDate = LocalDate.parse("2025-01-01"),
            departureTime = LocalTime.parse("09:00"),
            vehicleID = "x",
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
        assertFalse(a == create(seq = 9999))
        assertFalse(create(seq = null) == create(seq = null))
    }

    @Test
    fun mutators() {
        val e = create()
        e.routeID = 2
        e.stopID = 2
        e.departureDate = LocalDate.parse("2025-02-02")
        e.departureTime = LocalTime.parse("10:00")
        e.vehicleID = "y"
    }
}
