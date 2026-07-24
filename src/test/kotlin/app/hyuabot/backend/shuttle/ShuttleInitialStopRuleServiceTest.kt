package app.hyuabot.backend.shuttle

import app.hyuabot.backend.database.entity.ShuttleInitialStopRule
import app.hyuabot.backend.database.repository.ShuttleInitialStopRuleRepository
import app.hyuabot.backend.database.repository.ShuttlePeriodTypeRepository
import app.hyuabot.backend.database.repository.ShuttleStopRepository
import app.hyuabot.backend.shuttle.domain.ShuttleGeoPoint
import app.hyuabot.backend.shuttle.domain.ShuttleInitialStopRuleRequest
import app.hyuabot.backend.shuttle.exception.InvalidShuttleInitialStopRuleException
import app.hyuabot.backend.shuttle.exception.ShuttleInitialStopRuleNotFoundException
import app.hyuabot.backend.shuttle.service.ShuttleInitialStopRuleService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class ShuttleInitialStopRuleServiceTest {
    @Mock lateinit var repository: ShuttleInitialStopRuleRepository

    @Mock lateinit var periodTypeRepository: ShuttlePeriodTypeRepository

    @Mock lateinit var stopRepository: ShuttleStopRepository

    @InjectMocks lateinit var service: ShuttleInitialStopRuleService

    private val triangle =
        listOf(
            ShuttleGeoPoint(37.29, 126.83),
            ShuttleGeoPoint(37.30, 126.83),
            ShuttleGeoPoint(37.30, 126.84),
        )

    private fun request(
        name: String = " Campus ",
        periodType: String = "semester",
        stopName: String = "station",
        startTime: LocalTime? = null,
        endTime: LocalTime? = null,
        polygon: List<ShuttleGeoPoint> = triangle,
    ) = ShuttleInitialStopRuleRequest(
        name = name,
        periodType = periodType,
        weekday = true,
        startTime = startTime,
        endTime = endTime,
        stopName = stopName,
        priority = 100,
        enabled = true,
        polygon = polygon,
    )

    private fun rule(
        seq: Int? = 1,
        startTime: LocalTime? = null,
        endTime: LocalTime? = null,
    ) = ShuttleInitialStopRule(
        seq = seq,
        name = "Campus",
        periodType = "semester",
        weekday = true,
        startTime = startTime,
        endTime = endTime,
        stopName = "station",
        priority = 100,
        enabled = true,
        polygon = triangle,
    )

    private fun allowReferences() {
        whenever(periodTypeRepository.existsById("semester")).thenReturn(true)
        whenever(stopRepository.existsById("station")).thenReturn(true)
    }

    @Test
    fun crud() {
        val existing = rule()
        whenever(repository.findAllByOrderByPriorityDescSeqAsc()).thenReturn(listOf(existing))
        whenever(repository.findById(1)).thenReturn(Optional.of(existing))
        whenever(repository.save(any<ShuttleInitialStopRule>())).thenAnswer { it.arguments[0] }
        allowReferences()

        assertEquals(listOf(existing), service.getAll())
        assertEquals(existing, service.get(1))
        assertEquals("Campus", service.create(request()).name)

        val updated = service.update(1, request(name = " Updated "))
        assertEquals("Updated", updated.name)
        service.delete(1)
        verify(repository).delete(existing)
    }

    @Test
    fun notFound() {
        whenever(repository.findById(404)).thenReturn(Optional.empty())
        assertThrows<ShuttleInitialStopRuleNotFoundException> { service.get(404) }
    }

    @Test
    fun activeRulesAndTimeRanges() {
        val allDay = rule(seq = 1)
        val morning = rule(seq = 2, startTime = LocalTime.of(7, 0), endTime = LocalTime.of(10, 0))
        val overnight = rule(seq = 3, startTime = LocalTime.of(22, 0), endTime = LocalTime.of(2, 0))
        val incomplete = rule(seq = 4, startTime = LocalTime.of(7, 0))
        whenever(
            repository.findByPeriodTypeInAndWeekdayInAndEnabledTrueOrderByPriorityDescSeqAsc(
                listOf("semester"),
                listOf(true),
            ),
        ).thenReturn(listOf(allDay, morning, overnight))

        assertEquals(2, service.getActive(listOf("semester"), listOf(true), LocalTime.of(8, 0), false).size)
        assertEquals(2, service.getActive(listOf("semester"), listOf(true), LocalTime.of(23, 0), false).size)
        assertTrue(service.isActiveAt(overnight, LocalTime.of(1, 59)))
        assertFalse(service.isActiveAt(overnight, LocalTime.of(2, 0)))
        assertTrue(service.isActiveAt(incomplete, LocalTime.NOON))
        assertTrue(service.getActive(emptyList(), listOf(true), LocalTime.NOON, false).isEmpty())
        assertTrue(service.getActive(listOf("semester"), emptyList(), LocalTime.NOON, false).isEmpty())
        assertTrue(service.getActive(listOf("semester"), listOf(true), LocalTime.NOON, true).isEmpty())
    }

    @Test
    fun rejectsInvalidFields() {
        assertInvalid(request(name = " "))
        assertInvalid(request(name = "x".repeat(81)))
        assertInvalid(request(periodType = "unknown"))

        whenever(periodTypeRepository.existsById("semester")).thenReturn(true)
        assertInvalid(request(stopName = "unknown"))

        whenever(stopRepository.existsById("station")).thenReturn(true)
        assertInvalid(request(startTime = LocalTime.NOON))
        assertInvalid(request(startTime = LocalTime.NOON, endTime = LocalTime.NOON))
    }

    @Test
    fun rejectsInvalidPolygons() {
        allowReferences()
        assertInvalid(request(polygon = triangle.take(2)))
        assertInvalid(request(polygon = List(101) { index -> ShuttleGeoPoint(37.0 + index * 0.0001, 126.0) }))
        assertInvalid(
            request(
                polygon =
                    listOf(
                        ShuttleGeoPoint(91.0, 126.0),
                        ShuttleGeoPoint(37.0, 126.0),
                        ShuttleGeoPoint(38.0, 127.0),
                    ),
            ),
        )
        assertInvalid(
            request(
                polygon =
                    listOf(
                        ShuttleGeoPoint(37.0, 126.0),
                        ShuttleGeoPoint(37.0, 126.0),
                        ShuttleGeoPoint(37.0, 126.0),
                    ),
            ),
        )
        assertInvalid(
            request(
                polygon =
                    listOf(
                        ShuttleGeoPoint(0.0, 0.0),
                        ShuttleGeoPoint(2.0, 2.0),
                        ShuttleGeoPoint(0.0, 2.0),
                        ShuttleGeoPoint(1.0, 0.0),
                    ),
            ),
        )
    }

    private fun assertInvalid(request: ShuttleInitialStopRuleRequest) {
        assertThrows<InvalidShuttleInitialStopRuleException> { service.create(request) }
    }
}
