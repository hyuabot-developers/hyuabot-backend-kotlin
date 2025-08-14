package app.hyuabot.backend.shuttle

import app.hyuabot.backend.database.entity.ShuttleTimetableView
import app.hyuabot.backend.database.repository.ShuttleTimetableViewRepository
import app.hyuabot.backend.shuttle.service.ShuttleTimetableService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.LocalTime
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class ShuttleTimetableServiceTest {
    @Mock private lateinit var shuttleTimetableViewRepository: ShuttleTimetableViewRepository

    @InjectMocks
    private lateinit var shuttleTimetableService: ShuttleTimetableService

    @Test
    @DisplayName("셔틀버스 시간표 (뷰) 목록 조회")
    fun getShuttleTimetable() {
        whenever(shuttleTimetableViewRepository.findAll())
            .thenReturn(
                listOf(
                    ShuttleTimetableView(
                        seq = 1,
                        periodType = "semester",
                        weekday = true,
                        routeName = "A",
                        routeTag = "A",
                        stopName = "Stop1",
                        departureTime = LocalTime.parse("09:00:00"),
                        destinationGroup = "Group1",
                    ),
                    ShuttleTimetableView(
                        seq = 2,
                        periodType = "semester",
                        weekday = true,
                        routeName = "B",
                        routeTag = "B",
                        stopName = "Stop2",
                        departureTime = LocalTime.parse("10:00:00"),
                        destinationGroup = "Group2",
                    ),
                ),
            )
        val result = shuttleTimetableService.getShuttleTimetable()
        assertEquals(2, result.size)
        assertEquals("A", result[0].routeName)
        assertEquals("Stop1", result[0].stopName)
        assertEquals(LocalTime.parse("09:00:00"), result[0].departureTime)
        assertEquals("B", result[1].routeName)
        assertEquals("Stop2", result[1].stopName)
        assertEquals(LocalTime.parse("10:00:00"), result[1].departureTime)
    }
}
