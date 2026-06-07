package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubwayRouteStationTest {
    private fun create(id: String = "x") =
        SubwayRouteStation(
            id = id,
            routeID = 1,
            name = "x",
            order = 1,
            cumulativeTime = Duration.ofMinutes(1),
            route = null,
            stationName = null,
            realtime = mutableListOf(),
            timetable = mutableListOf(),
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
        assertFalse(a == create(id = "y"))
    }
}
