package app.hyuabot.backend.shuttle

import app.hyuabot.backend.database.entity.ShuttleStop
import app.hyuabot.backend.database.repository.ShuttleStopRepository
import app.hyuabot.backend.shuttle.domain.CreateShuttleStopRequest
import app.hyuabot.backend.shuttle.domain.UpdateShuttleStopRequest
import app.hyuabot.backend.shuttle.exception.DuplicateShuttleStopException
import app.hyuabot.backend.shuttle.exception.ShuttleStopNotFoundException
import app.hyuabot.backend.shuttle.service.ShuttleStopService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class ShuttleStopServiceTest {
    @Mock
    private lateinit var shuttleStopRepository: ShuttleStopRepository

    @InjectMocks
    private lateinit var shuttleStopService: ShuttleStopService

    @Test
    @DisplayName("셔틀버스 정류장 목록 조회")
    fun getAllStops() {
        whenever(shuttleStopRepository.findAll()).thenReturn(
            listOf(
                ShuttleStop(
                    name = "Stop1",
                    latitude = 37.5665,
                    longitude = 126.978,
                    route = emptyList(),
                    routeToStart = emptyList(),
                    routeToEnd = emptyList(),
                ),
                ShuttleStop(
                    name = "Stop2",
                    latitude = 37.5670,
                    longitude = 126.979,
                    route = emptyList(),
                    routeToStart = emptyList(),
                    routeToEnd = emptyList(),
                ),
            ),
        )
        val result = shuttleStopService.getAllStops()
        assertEquals(2, result.size)
        val resultWithEmptyName = shuttleStopService.getAllStops("")
        assertEquals(2, resultWithEmptyName.size)
    }

    @Test
    @DisplayName("셔틀버스 정류장 목록 조회 (이름 필터링)")
    fun getStopsByName() {
        val nameFilter = "Stop1"
        whenever(shuttleStopRepository.findByNameContaining(nameFilter)).thenReturn(
            listOf(
                ShuttleStop(
                    name = "Stop1",
                    latitude = 37.5665,
                    longitude = 126.978,
                    route = emptyList(),
                    routeToStart = emptyList(),
                    routeToEnd = emptyList(),
                ),
            ),
        )
        val result = shuttleStopService.getAllStops(nameFilter)
        assertEquals(1, result.size)
        assertEquals("Stop1", result[0].name)
    }

    @Test
    @DisplayName("셔틀버스 정류장 이름으로 조회")
    fun getStopByName() {
        val stopName = "Stop1"
        val expectedStop =
            ShuttleStop(
                name = stopName,
                latitude = 37.5665,
                longitude = 126.978,
                route = emptyList(),
                routeToStart = emptyList(),
                routeToEnd = emptyList(),
            )
        whenever(shuttleStopRepository.findById(stopName)).thenReturn(Optional.of(expectedStop))

        val result = shuttleStopService.getStopByName(stopName)
        assertEquals(expectedStop, result)
    }

    @Test
    @DisplayName("셔틀버스 정류장 이름으로 조회 실패")
    fun getStopByNameNotFound() {
        val stopName = "NonExistentStop"
        whenever(shuttleStopRepository.findById(stopName)).thenReturn(Optional.empty())
        assertThrows<ShuttleStopNotFoundException> {
            shuttleStopService.getStopByName(stopName)
        }
    }

    @Test
    @DisplayName("셔틀버스 정류장 생성")
    fun createStop() {
        val newStop =
            ShuttleStop(
                name = "NewStop",
                latitude = 37.5670,
                longitude = 126.979,
                route = emptyList(),
                routeToStart = emptyList(),
                routeToEnd = emptyList(),
            )
        whenever(shuttleStopRepository.save(newStop)).thenReturn(newStop)
        val result =
            shuttleStopService.createStop(
                CreateShuttleStopRequest(
                    name = newStop.name,
                    latitude = newStop.latitude,
                    longitude = newStop.longitude,
                ),
            )
        assertEquals(newStop, result)
    }

    @Test
    @DisplayName("셔틀버스 정류장 생성 실패 (중복 이름)")
    fun createStopDuplicateName() {
        val existingStop =
            ShuttleStop(
                name = "ExistingStop",
                latitude = 37.5665,
                longitude = 126.978,
                route = emptyList(),
                routeToStart = emptyList(),
                routeToEnd = emptyList(),
            )
        whenever(shuttleStopRepository.findByName(existingStop.name)).thenReturn(existingStop)

        assertThrows<DuplicateShuttleStopException> {
            shuttleStopService.createStop(
                CreateShuttleStopRequest(
                    name = existingStop.name,
                    latitude = existingStop.latitude,
                    longitude = existingStop.longitude,
                ),
            )
        }
    }

    @Test
    @DisplayName("셔틀버스 정류장 수정")
    fun updateStop() {
        val existingStop =
            ShuttleStop(
                name = "ExistingStop",
                latitude = 37.5665,
                longitude = 126.978,
                route = emptyList(),
                routeToStart = emptyList(),
                routeToEnd = emptyList(),
            )
        val updatedLatitude = 37.5670
        val updatedLongitude = 126.979

        whenever(shuttleStopRepository.findById(existingStop.name)).thenReturn(Optional.of(existingStop))
        whenever(shuttleStopRepository.save(existingStop)).thenReturn(
            existingStop.apply {
                latitude = updatedLatitude
                longitude = updatedLongitude
            },
        )

        val result =
            shuttleStopService.updateStop(
                existingStop.name,
                UpdateShuttleStopRequest(
                    latitude = updatedLatitude,
                    longitude = updatedLongitude,
                ),
            )
        assertEquals(updatedLatitude, result.latitude)
        assertEquals(updatedLongitude, result.longitude)
    }

    @Test
    @DisplayName("셔틀버스 정류장 수정 실패 (정류장 없음)")
    fun updateStopNotFound() {
        val stopName = "NonExistentStop"
        whenever(shuttleStopRepository.findById(stopName)).thenReturn(Optional.empty())
        assertThrows<ShuttleStopNotFoundException> {
            shuttleStopService.updateStop(
                stopName,
                UpdateShuttleStopRequest(
                    latitude = 37.5670,
                    longitude = 126.979,
                ),
            )
        }
    }

    @Test
    @DisplayName("셔틀버스 정류장 삭제")
    fun deleteStopByName() {
        val stopName = "StopToDelete"
        val existingStop =
            ShuttleStop(
                name = stopName,
                latitude = 37.5665,
                longitude = 126.978,
                route = emptyList(),
                routeToStart = emptyList(),
                routeToEnd = emptyList(),
            )
        whenever(shuttleStopRepository.findByName(stopName)).thenReturn(existingStop)
        shuttleStopService.deleteStopByName(stopName)
        verify(shuttleStopRepository).delete(existingStop)
    }

    @Test
    @DisplayName("셔틀버스 정류장 삭제 실패 (정류장 없음)")
    fun deleteStopByNameNotFound() {
        val stopName = "NonExistentStop"
        whenever(shuttleStopRepository.findByName(stopName)).thenReturn(null)
        assertThrows<ShuttleStopNotFoundException> {
            shuttleStopService.deleteStopByName(stopName)
        }
    }
}
