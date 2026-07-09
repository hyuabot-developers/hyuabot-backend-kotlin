package app.hyuabot.backend.shuttle

import app.hyuabot.backend.database.entity.ShuttleHoliday
import app.hyuabot.backend.database.exception.LocalDateNotValidException
import app.hyuabot.backend.database.repository.ShuttleHolidayRepository
import app.hyuabot.backend.shuttle.domain.ShuttleHolidayRequest
import app.hyuabot.backend.shuttle.exception.DuplicateShuttleHolidayException
import app.hyuabot.backend.shuttle.exception.ShuttleHolidayNotFoundException
import app.hyuabot.backend.shuttle.service.ShuttleHolidayService
import com.github.usingsky.calendar.KoreanLunarCalendar
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argThat
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Optional
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class ShuttleHolidayServiceTest {
    @Mock
    private lateinit var repository: ShuttleHolidayRepository

    @InjectMocks
    private lateinit var service: ShuttleHolidayService

    @Test
    @DisplayName("셔틀 공휴일 목록 조회")
    fun testGetShuttleHolidayList() {
        whenever(repository.findAll()).thenReturn(
            listOf(
                ShuttleHoliday(
                    seq = 1,
                    date = LocalDate.of(2024, 1, 1),
                    calendarType = "solar",
                    type = "New Year's Day",
                ),
                ShuttleHoliday(
                    seq = 2,
                    date = LocalDate.of(2024, 2, 9),
                    calendarType = "lunar",
                    type = "Lunar New Year",
                ),
            ),
        )
        val holidays = service.getShuttleHolidayList()
        assertEquals(2, holidays.size)
        assertEquals(1, holidays[0].seq)
        assertEquals("2024-01-01", holidays[0].date.toString())
        assertEquals("solar", holidays[0].calendarType)
        assertEquals("New Year's Day", holidays[0].type)
        assertEquals(2, holidays[1].seq)
        assertEquals("2024-02-09", holidays[1].date.toString())
        assertEquals("lunar", holidays[1].calendarType)
        assertEquals("Lunar New Year", holidays[1].type)
    }

    @Test
    @DisplayName("셔틀 공휴일 항목 생성")
    fun testCreateShuttleHoliday() {
        val newHoliday =
            ShuttleHoliday(
                seq = 3,
                date = LocalDate.of(2024, 3, 1),
                calendarType = "solar",
                type = "Independence Movement Day",
            )
        whenever(
            repository.save(
                argThat<ShuttleHoliday> {
                    date == LocalDate.of(2024, 3, 1) &&
                        calendarType == "solar" &&
                        type == "Independence Movement Day"
                },
            ),
        ).thenReturn(newHoliday)
        val createdHoliday =
            service.createShuttleHoliday(
                ShuttleHolidayRequest(
                    date = "2024-03-01",
                    calendarType = "solar",
                    type = "Independence Movement Day",
                ),
            )
        assertEquals(3, createdHoliday.seq)
        assertEquals("2024-03-01", createdHoliday.date.toString())
        assertEquals("solar", createdHoliday.calendarType)
        assertEquals("Independence Movement Day", createdHoliday.type)
    }

    @Test
    @DisplayName("셔틀 공휴일 생성 - 잘못된 날짜 형식")
    fun testCreateShuttleHolidayInvalidDate() {
        assertThrows<LocalDateNotValidException> {
            service.createShuttleHoliday(
                ShuttleHolidayRequest(
                    date = "2024-31-12",
                    calendarType = "solar",
                    type = "Invalid Date Test",
                ),
            )
        }
    }

    @Test
    @DisplayName("셔틀 공휴일 생성 - 중복된 날짜")
    fun testCreateShuttleHolidayDuplicateDate() {
        whenever(
            repository.findByDateAndCalendarType(
                date = LocalDate.of(2024, 1, 1),
                calendarType = "solar",
            ),
        ).thenReturn(
            ShuttleHoliday(
                seq = 1,
                date = LocalDate.of(2024, 1, 1),
                calendarType = "solar",
                type = "New Year's Day",
            ),
        )
        assertThrows<DuplicateShuttleHolidayException> {
            service.createShuttleHoliday(
                ShuttleHolidayRequest(
                    date = "2024-01-01",
                    calendarType = "solar",
                    type = "Duplicate Date Test",
                ),
            )
        }
    }

    @Test
    @DisplayName("셔틀 공휴일 항목 조회 by ID")
    fun testGetShuttleHolidayById() {
        val holiday =
            ShuttleHoliday(
                seq = 1,
                date = LocalDate.of(2024, 1, 1),
                calendarType = "solar",
                type = "New Year's Day",
            )
        whenever(repository.findById(1)).thenReturn(Optional.of(holiday))
        val fetchedHoliday = service.getShuttleHolidayById(1)
        assertEquals(1, fetchedHoliday.seq)
        assertEquals("2024-01-01", fetchedHoliday.date.toString())
        assertEquals("solar", fetchedHoliday.calendarType)
        assertEquals("New Year's Day", fetchedHoliday.type)
    }

    @Test
    @DisplayName("셔틀 공휴일 항목 조회 by ID - 존재하지 않는 ID")
    fun testGetShuttleHolidayByIdNotFound() {
        whenever(repository.findById(999)).thenReturn(Optional.empty())
        assertThrows<ShuttleHolidayNotFoundException> {
            service.getShuttleHolidayById(999)
        }
    }

    @Test
    @DisplayName("셔틀 공휴일 항목 수정")
    fun testUpdateShuttleHoliday() {
        val existingHoliday =
            ShuttleHoliday(
                seq = 1,
                date = LocalDate.of(2024, 1, 1),
                calendarType = "solar",
                type = "New Year's Day",
            )
        val updatedHoliday =
            ShuttleHoliday(
                seq = 1,
                date = LocalDate.of(2024, 1, 2),
                calendarType = "solar",
                type = "New Year's Holiday",
            )
        whenever(repository.findById(1)).thenReturn(Optional.of(existingHoliday))
        whenever(
            repository.findBySeqNotAndDateAndCalendarType(
                seq = 1,
                date = LocalDate.of(2024, 1, 2),
                calendarType = "solar",
            ),
        ).thenReturn(null)
        whenever(repository.save(existingHoliday)).thenReturn(updatedHoliday)
        val result =
            service.updateShuttleHoliday(
                1,
                ShuttleHolidayRequest(
                    date = "2024-01-02",
                    calendarType = "solar",
                    type = "New Year's Holiday",
                ),
            )
        assertEquals(1, result.seq)
        assertEquals("2024-01-02", result.date.toString())
        assertEquals("solar", result.calendarType)
        assertEquals("New Year's Holiday", result.type)
    }

    @Test
    @DisplayName("셔틀 공휴일 항목 수정 - 잘못된 날짜 형식")
    fun testUpdateShuttleHolidayInvalidDate() {
        assertThrows<LocalDateNotValidException> {
            service.updateShuttleHoliday(
                1,
                ShuttleHolidayRequest(
                    date = "2024-31-12",
                    calendarType = "solar",
                    type = "Invalid Date Test",
                ),
            )
        }
    }

    @Test
    @DisplayName("셔틀 공휴일 항목 수정 - 존재하지 않는 ID")
    fun testUpdateShuttleHolidayNotFound() {
        whenever(repository.findById(999)).thenReturn(Optional.empty())
        assertThrows<ShuttleHolidayNotFoundException> {
            service.updateShuttleHoliday(
                999,
                ShuttleHolidayRequest(
                    date = "2024-01-01",
                    calendarType = "solar",
                    type = "Non-existent ID Test",
                ),
            )
        }
    }

    @Test
    @DisplayName("셔틀 공휴일 항목 수정 - 중복된 날짜")
    fun testUpdateShuttleHolidayDuplicateDate() {
        val existingHoliday =
            ShuttleHoliday(
                seq = 1,
                date = LocalDate.of(2024, 1, 1),
                calendarType = "solar",
                type = "New Year's Day",
            )
        whenever(repository.findById(1)).thenReturn(Optional.of(existingHoliday))
        whenever(
            repository.findBySeqNotAndDateAndCalendarType(
                seq = 1,
                date = LocalDate.of(2024, 2, 9),
                calendarType = "lunar",
            ),
        ).thenReturn(
            ShuttleHoliday(
                seq = 2,
                date = LocalDate.of(2024, 2, 9),
                calendarType = "lunar",
                type = "Lunar New Year",
            ),
        )
        assertThrows<DuplicateShuttleHolidayException> {
            service.updateShuttleHoliday(
                1,
                ShuttleHolidayRequest(
                    date = "2024-02-09",
                    calendarType = "lunar",
                    type = "Duplicate Date Test",
                ),
            )
        }
    }

    @Test
    @DisplayName("셔틀 공휴일 항목 삭제")
    fun testDeleteShuttleHoliday() {
        val existingHoliday =
            ShuttleHoliday(
                seq = 1,
                date = LocalDate.of(2024, 1, 1),
                calendarType = "solar",
                type = "New Year's Day",
            )
        whenever(repository.findById(1)).thenReturn(Optional.of(existingHoliday))
        service.deleteShuttleHoliday(1)
    }

    @Test
    @DisplayName("셔틀 공휴일 항목 삭제 - 존재하지 않는 ID")
    fun testDeleteShuttleHolidayNotFound() {
        whenever(repository.findById(999)).thenReturn(Optional.empty())
        assertThrows<ShuttleHolidayNotFoundException> {
            service.deleteShuttleHoliday(999)
        }
    }

    @Test
    @DisplayName("셔틀 공휴일 항목 검색 - 날짜")
    fun testFindShuttleHoliday() {
        val lunarDate = KoreanLunarCalendar.getInstance()
        val solarDate = LocalDate.of(2024, 1, 1)
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        lunarDate.setSolarDate(2024, 1, 1)
        whenever(
            repository.findBySolarDateOrLunarDate(
                solarDate,
                LocalDate.parse(lunarDate.lunarIsoFormat, dateFormat),
            ),
        ).thenReturn(
            ShuttleHoliday(
                seq = 1,
                date = solarDate,
                calendarType = "solar",
                type = "New Year's Day",
            ),
        )
        val result = service.findShuttleHoliday(solarDate)
        assertEquals(1, result?.seq)
        assertEquals("2024-01-01", result?.date.toString())
        assertEquals("solar", result?.calendarType)
        assertEquals("New Year's Day", result?.type)
    }

    @Test
    @DisplayName("셔틀 공휴일 발생일 범위 검색")
    fun testFindShuttleHolidayOccurrences() {
        val start = LocalDate.of(2026, 3, 1)
        val end = LocalDate.of(2026, 3, 3)
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        listOf(start, start.plusDays(1), start.plusDays(2)).forEach { date ->
            val lunarDate = KoreanLunarCalendar.getInstance()
            lunarDate.setSolarDate(date.year, date.monthValue, date.dayOfMonth)
            whenever(
                repository.findBySolarDateOrLunarDate(
                    date,
                    LocalDate.parse(lunarDate.lunarIsoFormat, dateFormat),
                ),
            ).thenReturn(
                if (date == start.plusDays(1)) {
                    ShuttleHoliday(
                        seq = 2,
                        date = date,
                        calendarType = "solar",
                        type = "halt",
                    )
                } else {
                    null
                },
            )
        }

        val result = service.findShuttleHolidayOccurrences(start, end)

        assertEquals(1, result.size)
        assertEquals(start.plusDays(1), result[0].date)
        assertEquals(2, result[0].holiday.seq)
        assertEquals("halt", result[0].holiday.type)
    }

    @Test
    @DisplayName("셔틀 공휴일 발생일 범위 검색 - 잘못된 범위")
    fun testFindShuttleHolidayOccurrencesInvalidRange() {
        assertThrows<IllegalArgumentException> {
            service.findShuttleHolidayOccurrences(
                LocalDate.of(2026, 3, 3),
                LocalDate.of(2026, 3, 1),
            )
        }
    }
}
