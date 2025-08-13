package app.hyuabot.backend.shuttle

import app.hyuabot.backend.database.entity.ShuttlePeriod
import app.hyuabot.backend.database.exception.LocalDateTimeNotValidException
import app.hyuabot.backend.database.repository.ShuttlePeriodRepository
import app.hyuabot.backend.shuttle.domain.ShuttlePeriodRequest
import app.hyuabot.backend.shuttle.exception.ShuttlePeriodNotFoundException
import app.hyuabot.backend.shuttle.service.ShuttlePeriodService
import app.hyuabot.backend.utility.LocalDateTimeBuilder
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.Optional
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class ShuttlePeriodServiceTest {
    @Mock
    private lateinit var shuttlePeriodRepository: ShuttlePeriodRepository

    @InjectMocks
    private lateinit var shuttlePeriodService: ShuttlePeriodService

    @Test
    @DisplayName("셔틀버스 운행 기간 목록 조회 테스트")
    fun getShuttlePeriodListTest() {
        whenever(shuttlePeriodRepository.findAll()).thenReturn(
            listOf(
                ShuttlePeriod(
                    seq = 1,
                    start = ZonedDateTime.parse("2025-09-01T00:00:00.000+09:00"),
                    end = ZonedDateTime.parse("2025-12-23T23:59:59.999+09:00"),
                    type = "semester",
                    periodType = null,
                ),
                ShuttlePeriod(
                    seq = 2,
                    start = ZonedDateTime.parse("2025-12-24T00:00:00.000+09:00"),
                    end = ZonedDateTime.parse("2026-01-15T23:59:59.999+09:00"),
                    type = "vacation_session",
                    periodType = null,
                ),
                ShuttlePeriod(
                    seq = 3,
                    start = ZonedDateTime.parse("2026-01-16T00:00:00.000+09:00"),
                    end = ZonedDateTime.parse("2026-02-28T23:59:59.999+09:00"),
                    type = "vacation",
                    periodType = null,
                ),
            ),
        )
        val periods = shuttlePeriodService.getShuttlePeriodList()
        assertEquals(3, periods.size)
        assertEquals("semester", periods[0].type)
        assertEquals("vacation_session", periods[1].type)
        assertEquals("vacation", periods[2].type)
        assertEquals(ZonedDateTime.parse("2025-09-01T00:00:00.000+09:00"), periods[0].start)
        assertEquals(ZonedDateTime.parse("2025-12-23T23:59:59.999+09:00"), periods[0].end)
        assertEquals(ZonedDateTime.parse("2025-12-24T00:00:00.000+09:00"), periods[1].start)
        assertEquals(ZonedDateTime.parse("2026-01-15T23:59:59.999+09:00"), periods[1].end)
        assertEquals(ZonedDateTime.parse("2026-01-16T00:00:00.000+09:00"), periods[2].start)
        assertEquals(ZonedDateTime.parse("2026-02-28T23:59:59.999+09:00"), periods[2].end)
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 생성 테스트")
    fun createShuttlePeriodTest() {
        val payload =
            ShuttlePeriodRequest(
                type = "semester",
                start = "2025-09-01 00:00:00",
                end = "2025-12-23 23:59:59",
            )
        val expectedPeriod =
            ShuttlePeriod(
                seq = null,
                type = payload.type,
                start = ZonedDateTime.parse("2025-09-01T00:00:00.000+09:00").withZoneSameLocal(LocalDateTimeBuilder.serviceTimezone),
                end = ZonedDateTime.parse("2025-12-23T23:59:59.999+09:00").withZoneSameLocal(LocalDateTimeBuilder.serviceTimezone),
                periodType = null,
            )

        whenever(shuttlePeriodRepository.save(expectedPeriod)).thenReturn(expectedPeriod)

        val createdPeriod = shuttlePeriodService.createShuttlePeriod(payload)
        assertEquals(expectedPeriod.type, createdPeriod.type)
        assertEquals(expectedPeriod.start, createdPeriod.start)
        assertEquals(expectedPeriod.end, createdPeriod.end)
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 생성 테스트 (시작이 종료보다 늦은 경우 예외 발생)")
    fun createShuttlePeriodInvalidTest() {
        val payload =
            ShuttlePeriodRequest(
                type = "semester",
                start = "2025-12-24 00:00:00",
                end = "2025-09-01 23:59:59",
            )

        assertThrows<IllegalArgumentException> { shuttlePeriodService.createShuttlePeriod(payload) }
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 생성 테스트 (시작 날짜가 잘못된 형식인 경우 예외 발생)")
    fun createShuttlePeriodStartInvalidFormatTest() {
        val payload =
            ShuttlePeriodRequest(
                type = "semester",
                start = "2025-09-01 00:00:60",
                end = "2025-12-23 23:59:59",
            )

        assertThrows<LocalDateTimeNotValidException> { shuttlePeriodService.createShuttlePeriod(payload) }
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 생성 테스트 (종료 날짜가 잘못된 형식인 경우 예외 발생)")
    fun createShuttlePeriodEndInvalidFormatTest() {
        val payload =
            ShuttlePeriodRequest(
                type = "semester",
                start = "2025-09-01 00:00:00",
                end = "2025-12-23 23:59:5",
            )

        assertThrows<LocalDateTimeNotValidException> { shuttlePeriodService.createShuttlePeriod(payload) }
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 조회 테스트")
    fun getShuttlePeriodTest() {
        val seq = 1
        val expectedPeriod =
            ShuttlePeriod(
                seq = seq,
                start = ZonedDateTime.parse("2025-09-01T00:00:00.000+09:00"),
                end = ZonedDateTime.parse("2025-12-23T23:59:59.999+09:00"),
                type = "semester",
                periodType = null,
            )

        whenever(shuttlePeriodRepository.findById(seq)).thenReturn(Optional.of(expectedPeriod))

        val period = shuttlePeriodService.getShuttlePeriod(seq)
        assertEquals(expectedPeriod.seq, period.seq)
        assertEquals(expectedPeriod.type, period.type)
        assertEquals(expectedPeriod.start, period.start)
        assertEquals(expectedPeriod.end, period.end)
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 조회 테스트 (존재하지 않는 경우 예외 발생)")
    fun getShuttlePeriodNotFoundTest() {
        val seq = 999

        whenever(shuttlePeriodRepository.findById(seq)).thenReturn(Optional.empty())

        assertThrows<ShuttlePeriodNotFoundException> { shuttlePeriodService.getShuttlePeriod(seq) }
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 수정 테스트")
    fun updateShuttlePeriodTest() {
        val seq = 1
        val payload =
            ShuttlePeriodRequest(
                type = "semester",
                start = "2025-09-01 00:00:00",
                end = "2025-12-23 23:59:59",
            )
        val existingPeriod =
            ShuttlePeriod(
                seq = seq,
                start = ZonedDateTime.parse("2025-09-01T00:00:00.000+09:00"),
                end = ZonedDateTime.parse("2025-12-23T23:59:59.999+09:00"),
                type = "vacation",
                periodType = null,
            )
        val updatedPeriod =
            ShuttlePeriod(
                seq = seq,
                start = ZonedDateTime.parse("2025-09-01T00:00:00.000+09:00").withZoneSameLocal(LocalDateTimeBuilder.serviceTimezone),
                end = ZonedDateTime.parse("2025-12-23T23:59:59.999+09:00").withZoneSameLocal(LocalDateTimeBuilder.serviceTimezone),
                type = payload.type,
                periodType = null,
            )

        whenever(shuttlePeriodRepository.findById(seq)).thenReturn(Optional.of(existingPeriod))
        whenever(shuttlePeriodRepository.save(updatedPeriod)).thenReturn(updatedPeriod)

        val result = shuttlePeriodService.updateShuttlePeriod(seq, payload)
        assertEquals(updatedPeriod.seq, result.seq)
        assertEquals(updatedPeriod.type, result.type)
        assertEquals(updatedPeriod.start, result.start)
        assertEquals(updatedPeriod.end, result.end)
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 수정 테스트 (시작 날짜가 잘못된 형식인 경우 예외 발생)")
    fun updateShuttlePeriodStartInvalidFormatTest() {
        val seq = 1
        val payload =
            ShuttlePeriodRequest(
                type = "semester",
                start = "2025-09-01 00:00:0",
                end = "2025-12-23 23:59:59",
            )

        assertThrows<LocalDateTimeNotValidException> { shuttlePeriodService.updateShuttlePeriod(seq, payload) }
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 수정 테스트 (종료 날짜가 잘못된 형식인 경우 예외 발생)")
    fun updateShuttlePeriodEndInvalidFormatTest() {
        val seq = 1
        val payload =
            ShuttlePeriodRequest(
                type = "semester",
                start = "2025-09-01 00:00:00",
                end = "2025-12-23 23:59:5",
            )

        assertThrows<LocalDateTimeNotValidException> { shuttlePeriodService.updateShuttlePeriod(seq, payload) }
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 수정 테스트 (존재하지 않는 경우 예외 발생)")
    fun updateShuttlePeriodNotFoundTest() {
        val seq = 999
        val payload =
            ShuttlePeriodRequest(
                type = "semester",
                start = "2025-09-01 00:00:00",
                end = "2025-12-23 23:59:59",
            )

        whenever(shuttlePeriodRepository.findById(seq)).thenReturn(Optional.empty())

        assertThrows<ShuttlePeriodNotFoundException> { shuttlePeriodService.updateShuttlePeriod(seq, payload) }
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 삭제 테스트")
    fun deleteShuttlePeriodTest() {
        val seq = 1
        val existingPeriod =
            ShuttlePeriod(
                seq = seq,
                start = ZonedDateTime.parse("2025-09-01T00:00:00.000+09:00"),
                end = ZonedDateTime.parse("2025-12-23T23:59:59.999+09:00"),
                type = "semester",
                periodType = null,
            )

        whenever(shuttlePeriodRepository.findById(seq)).thenReturn(Optional.of(existingPeriod))

        shuttlePeriodService.deleteShuttlePeriod(seq)

        verify(shuttlePeriodRepository).delete(existingPeriod)
    }

    @Test
    @DisplayName("셔틀버스 운행 기간 삭제 테스트 (존재하지 않는 경우 예외 발생)")
    fun deleteShuttlePeriodNotFoundTest() {
        val seq = 999

        whenever(shuttlePeriodRepository.findById(seq)).thenReturn(Optional.empty())

        assertThrows<ShuttlePeriodNotFoundException> { shuttlePeriodService.deleteShuttlePeriod(seq) }
    }
}
