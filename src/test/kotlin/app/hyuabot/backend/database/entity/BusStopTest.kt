package app.hyuabot.backend.database.entity

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BusStopTest {
    private fun create(id: Int = 1) =
        BusStop(
            id = id,
            name = "x",
            districtCode = 1,
            mobileNumber = "x",
            regionName = "x",
            latitude = 0.0,
            longitude = 0.0,
            busRoutes = mutableListOf(),
            startBusRoutes = mutableListOf(),
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
