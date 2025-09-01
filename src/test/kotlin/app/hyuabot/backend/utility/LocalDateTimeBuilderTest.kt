package app.hyuabot.backend.utility

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LocalDateTimeBuilderTest {
    @Test
    @DisplayName("시작 일자 및 종료 일자의 날짜 형식이 맞지 않을 때")
    fun invalidDateRangeFormat() {
        assertEquals(false, LocalDateTimeBuilder.checkLocalDateRange("2023-10-32", "2023-10-31"))
        assertEquals(false, LocalDateTimeBuilder.checkLocalDateRange("2023-10-30", "2023-10-32"))
        assertEquals(true, LocalDateTimeBuilder.checkLocalDateRange("2023-10-30", "2023-10-31"))
    }

    @Test
    @DisplayName("날짜 형식이 맞지 않을 때")
    fun invalidDateFormat() {
        assertEquals(false, LocalDateTimeBuilder.checkLocalDateFormat("2023-10-32"))
        assertEquals(false, LocalDateTimeBuilder.checkLocalDateFormat("202-13-31"))
        assertEquals(true, LocalDateTimeBuilder.checkLocalDateFormat("2023-10-31"))
    }
}
