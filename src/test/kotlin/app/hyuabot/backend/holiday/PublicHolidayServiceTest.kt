package app.hyuabot.backend.holiday

import app.hyuabot.backend.database.entity.PublicHoliday
import app.hyuabot.backend.database.exception.LocalDateNotValidException
import app.hyuabot.backend.database.repository.PublicHolidayRepository
import app.hyuabot.backend.holiday.domain.PublicHolidayRequest
import app.hyuabot.backend.holiday.exception.DuplicatePublicHolidayException
import app.hyuabot.backend.holiday.exception.PublicHolidayNotFoundException
import app.hyuabot.backend.holiday.service.PublicHolidayService
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
class PublicHolidayServiceTest {
    @Mock
    private lateinit var repository: PublicHolidayRepository

    @InjectMocks
    private lateinit var service: PublicHolidayService

    @Test
    @DisplayName("공휴일 목록 조회")
    fun testGetPublicHolidayList() {
        whenever(repository.findAll()).thenReturn(
            listOf(
                PublicHoliday(seq = 1, date = LocalDate.of(2025, 1, 1), name = "신정", calendarType = "solar"),
                PublicHoliday(seq = 2, date = LocalDate.of(2025, 1, 29), name = "설날", calendarType = "lunar"),
            ),
        )
        val holidays = service.getPublicHolidayList()
        assertEquals(2, holidays.size)
        assertEquals(1, holidays[0].seq)
        assertEquals("2025-01-01", holidays[0].date.toString())
        assertEquals("신정", holidays[0].name)
        assertEquals("solar", holidays[0].calendarType)
    }

    @Test
    @DisplayName("공휴일 항목 생성")
    fun testCreatePublicHoliday() {
        val newHoliday = PublicHoliday(seq = 1, date = LocalDate.of(2025, 3, 1), name = "삼일절", calendarType = "solar")
        whenever(
            repository.save(
                argThat<PublicHoliday> {
                    date == LocalDate.of(2025, 3, 1) && name == "삼일절" && calendarType == "solar"
                },
            ),
        ).thenReturn(newHoliday)
        val created = service.createPublicHoliday(PublicHolidayRequest(date = "2025-03-01", name = "삼일절", calendarType = "solar"))
        assertEquals(1, created.seq)
        assertEquals("2025-03-01", created.date.toString())
        assertEquals("삼일절", created.name)
        assertEquals("solar", created.calendarType)
    }

    @Test
    @DisplayName("공휴일 생성 - 잘못된 날짜 형식")
    fun testCreatePublicHolidayInvalidDate() {
        assertThrows<LocalDateNotValidException> {
            service.createPublicHoliday(PublicHolidayRequest(date = "2025-31-12", name = "테스트", calendarType = "solar"))
        }
    }

    @Test
    @DisplayName("공휴일 생성 - 중복된 날짜")
    fun testCreatePublicHolidayDuplicateDate() {
        whenever(
            repository.findByDateAndCalendarType(date = LocalDate.of(2025, 1, 1), calendarType = "solar"),
        ).thenReturn(
            PublicHoliday(seq = 1, date = LocalDate.of(2025, 1, 1), name = "신정", calendarType = "solar"),
        )
        assertThrows<DuplicatePublicHolidayException> {
            service.createPublicHoliday(PublicHolidayRequest(date = "2025-01-01", name = "중복 테스트", calendarType = "solar"))
        }
    }

    @Test
    @DisplayName("공휴일 항목 조회 by ID")
    fun testGetPublicHolidayById() {
        val holiday = PublicHoliday(seq = 1, date = LocalDate.of(2025, 1, 1), name = "신정", calendarType = "solar")
        whenever(repository.findById(1)).thenReturn(Optional.of(holiday))
        val result = service.getPublicHolidayById(1)
        assertEquals(1, result.seq)
        assertEquals("2025-01-01", result.date.toString())
        assertEquals("신정", result.name)
        assertEquals("solar", result.calendarType)
    }

    @Test
    @DisplayName("공휴일 항목 조회 by ID - 존재하지 않는 ID")
    fun testGetPublicHolidayByIdNotFound() {
        whenever(repository.findById(999)).thenReturn(Optional.empty())
        assertThrows<PublicHolidayNotFoundException> {
            service.getPublicHolidayById(999)
        }
    }

    @Test
    @DisplayName("공휴일 항목 수정")
    fun testUpdatePublicHoliday() {
        val existing = PublicHoliday(seq = 1, date = LocalDate.of(2025, 1, 1), name = "신정", calendarType = "solar")
        val updated = PublicHoliday(seq = 1, date = LocalDate.of(2025, 1, 2), name = "신정 대체공휴일", calendarType = "solar")
        whenever(repository.findById(1)).thenReturn(Optional.of(existing))
        whenever(
            repository.findBySeqNotAndDateAndCalendarType(seq = 1, date = LocalDate.of(2025, 1, 2), calendarType = "solar"),
        ).thenReturn(null)
        whenever(repository.save(existing)).thenReturn(updated)
        val result =
            service.updatePublicHoliday(
                1,
                PublicHolidayRequest(date = "2025-01-02", name = "신정 대체공휴일", calendarType = "solar"),
            )
        assertEquals(1, result.seq)
        assertEquals("2025-01-02", result.date.toString())
        assertEquals("신정 대체공휴일", result.name)
    }

    @Test
    @DisplayName("공휴일 항목 수정 - 잘못된 날짜 형식")
    fun testUpdatePublicHolidayInvalidDate() {
        assertThrows<LocalDateNotValidException> {
            service.updatePublicHoliday(1, PublicHolidayRequest(date = "2025-31-12", name = "테스트", calendarType = "solar"))
        }
    }

    @Test
    @DisplayName("공휴일 항목 수정 - 존재하지 않는 ID")
    fun testUpdatePublicHolidayNotFound() {
        whenever(repository.findById(999)).thenReturn(Optional.empty())
        assertThrows<PublicHolidayNotFoundException> {
            service.updatePublicHoliday(999, PublicHolidayRequest(date = "2025-01-01", name = "테스트", calendarType = "solar"))
        }
    }

    @Test
    @DisplayName("공휴일 항목 수정 - 중복된 날짜")
    fun testUpdatePublicHolidayDuplicateDate() {
        val existing = PublicHoliday(seq = 1, date = LocalDate.of(2025, 1, 1), name = "신정", calendarType = "solar")
        whenever(repository.findById(1)).thenReturn(Optional.of(existing))
        whenever(
            repository.findBySeqNotAndDateAndCalendarType(seq = 1, date = LocalDate.of(2025, 3, 1), calendarType = "solar"),
        ).thenReturn(
            PublicHoliday(seq = 2, date = LocalDate.of(2025, 3, 1), name = "삼일절", calendarType = "solar"),
        )
        assertThrows<DuplicatePublicHolidayException> {
            service.updatePublicHoliday(1, PublicHolidayRequest(date = "2025-03-01", name = "중복 테스트", calendarType = "solar"))
        }
    }

    @Test
    @DisplayName("공휴일 항목 삭제")
    fun testDeletePublicHoliday() {
        val existing = PublicHoliday(seq = 1, date = LocalDate.of(2025, 1, 1), name = "신정", calendarType = "solar")
        whenever(repository.findById(1)).thenReturn(Optional.of(existing))
        service.deletePublicHoliday(1)
    }

    @Test
    @DisplayName("공휴일 항목 삭제 - 존재하지 않는 ID")
    fun testDeletePublicHolidayNotFound() {
        whenever(repository.findById(999)).thenReturn(Optional.empty())
        assertThrows<PublicHolidayNotFoundException> {
            service.deletePublicHoliday(999)
        }
    }

    @Test
    @DisplayName("공휴일 검색 - 양력 날짜")
    fun testFindPublicHoliday() {
        val lunarDate = KoreanLunarCalendar.getInstance()
        val solarDate = LocalDate.of(2025, 1, 1)
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        lunarDate.setSolarDate(2025, 1, 1)
        whenever(
            repository.findBySolarDateOrLunarDate(
                solarDate,
                LocalDate.parse(lunarDate.lunarIsoFormat, dateFormat),
            ),
        ).thenReturn(
            PublicHoliday(seq = 1, date = solarDate, name = "신정", calendarType = "solar"),
        )
        val result = service.findPublicHoliday(solarDate)
        assertEquals(1, result?.seq)
        assertEquals("2025-01-01", result?.date.toString())
        assertEquals("신정", result?.name)
    }

    @Test
    @DisplayName("공휴일 검색 - 공휴일 아닌 날짜")
    fun testFindPublicHolidayNotFound() {
        val lunarDate = KoreanLunarCalendar.getInstance()
        val solarDate = LocalDate.of(2025, 3, 3)
        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        lunarDate.setSolarDate(2025, 3, 3)
        whenever(
            repository.findBySolarDateOrLunarDate(
                solarDate,
                LocalDate.parse(lunarDate.lunarIsoFormat, dateFormat),
            ),
        ).thenReturn(null)
        val result = service.findPublicHoliday(solarDate)
        assertEquals(null, result)
    }

    @Test
    @DisplayName("공휴일 생성 - 잘못된 달력 유형")
    fun testCreatePublicHolidayInvalidCalendarType() {
        assertThrows<IllegalArgumentException> {
            service.createPublicHoliday(
                PublicHolidayRequest(date = "2026-01-01", name = "테스트", calendarType = "gregorian"),
            )
        }
    }

    @Test
    @DisplayName("공휴일 수정 - 잘못된 달력 유형")
    fun testUpdatePublicHolidayInvalidCalendarType() {
        assertThrows<IllegalArgumentException> {
            service.updatePublicHoliday(
                1,
                PublicHolidayRequest(date = "2026-01-01", name = "테스트", calendarType = "gregorian"),
            )
        }
    }
}
