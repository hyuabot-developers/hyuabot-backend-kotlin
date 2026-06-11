package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BusRouteTest {
    private fun stop() =
        BusStop(
            id = 1,
            name = "x",
            districtCode = 1,
            mobileNumber = "x",
            regionName = "x",
            latitude = 0.0,
            longitude = 0.0,
            busRoutes = mutableListOf(),
            startBusRoutes = mutableListOf(),
        )

    private fun create(id: Int = 1) =
        BusRoute(
            id = id,
            name = "x",
            typeCode = "x",
            typeName = "x",
            startStopID = 1,
            endStopID = 1,
            upFirstTime = LocalTime.parse("09:00"),
            upLastTime = LocalTime.parse("09:00"),
            downFirstTime = LocalTime.parse("09:00"),
            downLastTime = LocalTime.parse("09:00"),
            districtCode = 1,
            companyID = 1,
            companyName = "x",
            companyPhone = "x",
            stop = mutableListOf(),
            startStop = stop(),
            endStop = stop(),
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
}
