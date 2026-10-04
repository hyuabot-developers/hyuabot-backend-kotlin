package app.hyuabot.backend.subway.controller

import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubwayAfterFilterTest {
    @Test
    fun `after cutoff handles midnight for timetable rows`() {
        assertFalse(isAtOrAfterSubwayTime(LocalTime.of(0, 10), LocalTime.of(0, 20)))
        assertTrue(isAtOrAfterSubwayTime(LocalTime.of(0, 30), LocalTime.of(0, 20)))
    }

    @Test
    fun `after cutoff handles a midnight crossing from current time`() {
        assertFalse(isArrivalAtOrAfterSubwayTime(20, LocalTime.of(23, 55), LocalTime.of(0, 20)))
        assertTrue(isArrivalAtOrAfterSubwayTime(25, LocalTime.of(23, 55), LocalTime.of(0, 20)))
    }
}
