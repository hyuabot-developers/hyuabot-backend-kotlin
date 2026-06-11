package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubwayRealtimeTest {
    private fun create(
        stationID: String = "x",
        heading: String = "x",
        order: Int = 1,
    ) = SubwayRealtime(
        stationID = stationID,
        heading = heading,
        order = order,
        location = "x",
        remainingStop = 1,
        remainingTime = Duration.ofMinutes(1),
        terminalStationID = "x",
        trainNumber = "x",
        updatedAt = ZonedDateTime.now(),
        isExpress = false,
        isLast = false,
        status = 1,
        station = null,
        terminalStation = null,
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
        assertFalse(a == create(stationID = "y"))
        assertFalse(a == create(heading = "y"))
        assertFalse(a == create(order = 9999))
    }

    @Test
    fun mutators() {
        val e = create()
        e.location = "y"
        e.remainingStop = 2
        e.remainingTime = Duration.ofMinutes(2)
        e.terminalStationID = "y"
        e.trainNumber = "y"
        e.updatedAt = ZonedDateTime.now()
        e.isExpress = true
        e.isLast = true
        e.status = 2
    }
}
