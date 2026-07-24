package app.hyuabot.backend.database.entity

import app.hyuabot.backend.shuttle.domain.ShuttleGeoPoint
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShuttleInitialStopRuleTest {
    private fun create(seq: Int? = 1) =
        ShuttleInitialStopRule(
            seq = seq,
            name = "Campus",
            periodType = "semester",
            weekday = true,
            startTime = null,
            endTime = null,
            stopName = "station",
            priority = 1,
            enabled = true,
            polygon =
                listOf(
                    ShuttleGeoPoint(0.0, 0.0),
                    ShuttleGeoPoint(1.0, 0.0),
                    ShuttleGeoPoint(0.0, 1.0),
                ),
            createdAt = OffsetDateTime.parse("2026-01-01T00:00:00+09:00"),
            updatedAt = OffsetDateTime.parse("2026-01-02T00:00:00+09:00"),
        )

    @Test
    fun equalsAndHashCode() {
        ShuttleInitialStopRule::class.java.getDeclaredConstructor().newInstance()
        val value = create()
        assertEquals(OffsetDateTime.parse("2026-01-01T00:00:00+09:00"), value.createdAt)
        value.createdAt = OffsetDateTime.parse("2026-01-03T00:00:00+09:00")
        assertEquals(OffsetDateTime.parse("2026-01-02T00:00:00+09:00"), value.updatedAt)
        value.updatedAt = OffsetDateTime.parse("2026-01-04T00:00:00+09:00")
        assertTrue(value == value)
        assertFalse(value.equals(null))
        assertFalse(value.equals(Any()))
        assertEquals(value, create())
        assertEquals(value.hashCode(), create().hashCode())
        assertFalse(value == create(2))
        assertFalse(create(null) == create(null))
    }
}
