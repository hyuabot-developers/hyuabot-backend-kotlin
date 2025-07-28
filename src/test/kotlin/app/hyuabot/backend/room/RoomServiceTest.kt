package app.hyuabot.backend.room

import app.hyuabot.backend.building.exception.BuildingNotFoundException
import app.hyuabot.backend.database.entity.Building
import app.hyuabot.backend.database.entity.Room
import app.hyuabot.backend.database.repository.BuildingRepository
import app.hyuabot.backend.database.repository.RoomRepository
import app.hyuabot.backend.room.domain.RoomRequest
import app.hyuabot.backend.room.exception.DuplicateRoomException
import app.hyuabot.backend.room.exception.RoomNotFoundException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class RoomServiceTest {
    @Mock
    lateinit var buildingRepository: BuildingRepository

    @Mock
    lateinit var roomRepository: RoomRepository

    @InjectMocks
    lateinit var roomService: RoomService

    @Test
    @DisplayName("강의실 목록 조회 테스트")
    fun getRoomListTest() {
        whenever(roomRepository.findAll()).thenReturn(
            listOf(
                Room(seq = 1, buildingName = "Building A", number = "101", name = "Room 101"),
                Room(seq = 2, buildingName = "Building B", number = "202", name = "Room 202"),
            ),
        )
        val rooms = roomService.getRoomList(null, null)
        assertEquals(2, rooms.size)
        assertEquals("Building A", rooms[0].buildingName)
        assertEquals("Room 101", rooms[0].name)
        assertEquals("Building B", rooms[1].buildingName)
        assertEquals("Room 202", rooms[1].name)
    }

    @Test
    @DisplayName("강의실 목록 조회 테스트 (건물 이름 필터링)")
    fun getRoomListByBuildingNameTest() {
        val buildingName = "Building A"
        whenever(roomRepository.findByBuildingName(buildingName)).thenReturn(
            listOf(
                Room(buildingName = buildingName, number = "101", name = "Room 101"),
                Room(buildingName = buildingName, number = "102", name = "Room 102"),
            ),
        )
        val rooms = roomService.getRoomList(buildingName, null)
        assertEquals(2, rooms.size)
        assertEquals("Building A", rooms[0].buildingName)
        assertEquals("Room 101", rooms[0].name)
        assertEquals("Building A", rooms[1].buildingName)
        assertEquals("Room 102", rooms[1].name)
    }

    @Test
    @DisplayName("강의실 목록 조회 테스트 (이름 필터링)")
    fun getRoomListByNameTest() {
        val name = "Room 101"
        whenever(roomRepository.findByNameContaining(name)).thenReturn(
            listOf(
                Room(seq = 1, buildingName = "Building A", number = "101", name = "Room 101"),
            ),
        )
        val rooms = roomService.getRoomList(null, name)
        assertEquals(1, rooms.size)
        assertEquals("Building A", rooms[0].buildingName)
        assertEquals("Room 101", rooms[0].name)
    }

    @Test
    @DisplayName("강의실 목록 조회 테스트 (건물 이름과 이름 필터링)")
    fun getRoomListByBuildingNameAndNameTest() {
        val buildingName = "Building A"
        val name = "Room 101"
        whenever(roomRepository.findByBuildingNameAndNameContaining(buildingName, name)).thenReturn(
            listOf(
                Room(seq = 1, buildingName = buildingName, number = "101", name = "Room 101"),
            ),
        )
        val rooms = roomService.getRoomList(buildingName, name)
        assertEquals(1, rooms.size)
        assertEquals("Building A", rooms[0].buildingName)
        assertEquals("Room 101", rooms[0].name)
    }

    @Test
    @DisplayName("강의실 ID로 조회 테스트")
    fun getRoomByIDTest() {
        val seq = 1
        val room = Room(seq = seq, buildingName = "Building A", number = "101", name = "Room 101")
        whenever(roomRepository.findById(seq)).thenReturn(Optional.of(room))

        val result = roomService.getRoomByID(seq)
        assertEquals(room, result)
    }

    @Test
    @DisplayName("강의실 ID로 조회 실패 테스트 (존재하지 않는 ID)")
    fun getRoomByIDNotFoundTest() {
        val seq = 999
        whenever(roomRepository.findById(seq)).thenReturn(Optional.empty())
        assertThrows<RoomNotFoundException> { roomService.getRoomByID(seq) }
    }

    @Test
    @DisplayName("강의실 생성 테스트")
    fun createRoomTest() {
        val payload = RoomRequest(buildingName = "Building A", number = "101", name = "Room 101")
        val building =
            Building(
                id = "Y001",
                name = "Building A",
                campusID = 1,
                latitude = 37.5665,
                longitude = 126.978,
                url = "http://example.com/building-a",
            )
        val room = Room(seq = null, buildingName = payload.buildingName, number = payload.number, name = payload.name)

        whenever(buildingRepository.findByName(payload.buildingName)).thenReturn(building)
        whenever(roomRepository.findByBuildingNameAndNumber(payload.buildingName, payload.number)).thenReturn(null)
        whenever(roomRepository.save(room)).thenReturn(room)

        val result = roomService.createRoom(payload)
        assertEquals(room, result)
    }

    @Test
    @DisplayName("강의실 생성 실패 테스트 (존재하지 않는 건물 이름)")
    fun createRoomBuildingNotFoundTest() {
        val payload = RoomRequest(buildingName = "Nonexistent Building", number = "101", name = "Room 101")
        whenever(buildingRepository.findByName(payload.buildingName)).thenReturn(null)

        assertThrows<BuildingNotFoundException> { roomService.createRoom(payload) }
    }

    @Test
    @DisplayName("강의실 생성 실패 테스트 (중복된 강의실 번호)")
    fun createRoomDuplicateTest() {
        val payload = RoomRequest(buildingName = "Building A", number = "101", name = "Room 101")
        val existingRoom = Room(seq = 1, buildingName = payload.buildingName, number = payload.number, name = "Existing Room")

        whenever(buildingRepository.findByName(payload.buildingName)).thenReturn(
            Building(
                id = "Y001",
                name = "Building A",
                campusID = 1,
                latitude = 37.5665,
                longitude = 126.978,
                url = "http://example.com/building-a",
            ),
        )
        whenever(roomRepository.findByBuildingNameAndNumber(payload.buildingName, payload.number)).thenReturn(existingRoom)

        assertThrows<DuplicateRoomException> { roomService.createRoom(payload) }
    }

    @Test
    @DisplayName("강의실 수정 테스트")
    fun updateRoomTest() {
        val seq = 1
        val payload = RoomRequest(buildingName = "Building A", number = "102", name = "Room 102")
        val existingRoom = Room(seq = seq, buildingName = "Building A", number = "101", name = "Room 101")

        whenever(buildingRepository.findByName(payload.buildingName)).thenReturn(
            Building(
                id = "Y001",
                name = "Building A",
                campusID = 1,
                latitude = 37.5665,
                longitude = 126.978,
                url = "http://example.com/building-a",
            ),
        )
        whenever(roomRepository.findById(seq)).thenReturn(Optional.of(existingRoom))
        whenever(roomRepository.save(existingRoom)).thenReturn(
            existingRoom.apply {
                buildingName = payload.buildingName
                number = payload.number
                name = payload.name
            },
        )

        val result = roomService.updateRoom(seq, payload)
        assertEquals("Building A", result.buildingName)
        assertEquals("102", result.number)
        assertEquals("Room 102", result.name)
    }

    @Test
    @DisplayName("강의실 수정 실패 테스트 (존재하지 않는 건물 이름)")
    fun updateRoomBuildingNotFoundTest() {
        val seq = 1
        val payload = RoomRequest(buildingName = "Nonexistent Building", number = "102", name = "Room 102")
        whenever(buildingRepository.findByName(payload.buildingName)).thenReturn(null)

        assertThrows<BuildingNotFoundException> { roomService.updateRoom(seq, payload) }
    }

    @Test
    @DisplayName("강의실 수정 실패 테스트 (존재하지 않는 강의실 ID)")
    fun updateRoomNotFoundTest() {
        val seq = 999
        val payload = RoomRequest(buildingName = "Building A", number = "102", name = "Room 102")
        whenever(buildingRepository.findByName(payload.buildingName)).thenReturn(
            Building(
                id = "Y001",
                name = "Building A",
                campusID = 1,
                latitude = 37.5665,
                longitude = 126.978,
                url = "http://example.com/building-a",
            ),
        )
        whenever(roomRepository.findById(seq)).thenReturn(Optional.empty())

        assertThrows<RoomNotFoundException> { roomService.updateRoom(seq, payload) }
    }

    @Test
    @DisplayName("강의실 삭제 테스트")
    fun deleteRoomTest() {
        val seq = 1
        val room = Room(seq = seq, buildingName = "Building A", number = "101", name = "Room 101")

        whenever(roomRepository.findById(seq)).thenReturn(Optional.of(room))
        roomService.deleteRoom(seq)
        verify(roomRepository).delete(room)
    }

    @Test
    @DisplayName("강의실 삭제 실패 테스트 (존재하지 않는 강의실 ID)")
    fun deleteRoomNotFoundTest() {
        val seq = 999
        whenever(roomRepository.findById(seq)).thenReturn(Optional.empty())
        assertThrows<RoomNotFoundException> { roomService.deleteRoom(seq) }
    }
}
