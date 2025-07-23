package app.hyuabot.backend.readingRoom

import app.hyuabot.backend.database.entity.Campus
import app.hyuabot.backend.database.entity.ReadingRoom
import app.hyuabot.backend.database.repository.ReadingRoomRepository
import app.hyuabot.backend.readingRoom.domain.CreateReadingRoomRequest
import app.hyuabot.backend.readingRoom.domain.UpdateReadingRoomRequest
import app.hyuabot.backend.readingRoom.exception.DuplicateReadingRoomException
import app.hyuabot.backend.readingRoom.exception.ReadingRoomNotFoundException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Optional
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class ReadingRoomServiceTest {
    @Mock
    lateinit var readingRoomRepository: ReadingRoomRepository

    @InjectMocks
    lateinit var readingRoomService: ReadingRoomService

    @Test
    @DisplayName("열람실 목록 조회 테스트")
    fun testGetReadingRoomList() {
        val now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
        whenever(readingRoomRepository.findAll()).thenReturn(
            listOf(
                ReadingRoom(
                    id = 1,
                    name = "열람실 A",
                    campusID = 1,
                    isActive = true,
                    isReservable = true,
                    total = 10,
                    active = 10,
                    occupied = 0,
                    updatedAt = now,
                    campus = Campus(id = 1, name = "서울"),
                ),
                ReadingRoom(
                    id = 2,
                    name = "열람실 B",
                    campusID = 2,
                    isActive = true,
                    isReservable = true,
                    total = 20,
                    active = 15,
                    occupied = 5,
                    updatedAt = now,
                    campus = Campus(id = 2, name = "ERICA"),
                ),
            ),
        )
        val readingRoomList = readingRoomService.getReadingRoomList()
        assertEquals(2, readingRoomList.size)
        assertEquals("열람실 A", readingRoomList[0].name)
        assertEquals("열람실 B", readingRoomList[1].name)
        assertEquals(1, readingRoomList[0].campusID)
        assertEquals(2, readingRoomList[1].campusID)
        assertEquals(true, readingRoomList[0].isActive)
        assertEquals(true, readingRoomList[1].isActive)
        assertEquals(true, readingRoomList[0].isReservable)
        assertEquals(true, readingRoomList[1].isReservable)
        assertEquals(10, readingRoomList[0].total)
        assertEquals(20, readingRoomList[1].total)
    }

    @Test
    @DisplayName("열람실 ID로 조회 테스트")
    fun testGetReadingRoomById() {
        val room =
            ReadingRoom(
                id = 1,
                name = "열람실 A",
                campusID = 1,
                isActive = true,
                isReservable = true,
                total = 10,
                active = 10,
                occupied = 0,
                updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul")),
                campus = Campus(id = 1, name = "서울"),
            )
        whenever(readingRoomRepository.findById(1)).thenReturn(Optional.of(room))

        val foundRoom = readingRoomService.getReadingRoomById(1)
        assertEquals("열람실 A", foundRoom.name)
    }

    @Test
    @DisplayName("열람실 ID로 조회 실패 테스트 (존재하지 않는 ID)")
    fun testGetReadingRoomByIdNotFound() {
        whenever(readingRoomRepository.findById(999)).thenReturn(Optional.empty())
        assertThrows<ReadingRoomNotFoundException> { readingRoomService.getReadingRoomById(999) }
    }

    @Test
    @DisplayName("열람실 ID로 수정 테스트")
    fun testUpdateReadingRoom() {
        val existingRoom =
            ReadingRoom(
                id = 1,
                name = "열람실 A",
                campusID = 1,
                isActive = true,
                isReservable = true,
                total = 10,
                active = 10,
                occupied = 0,
                updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul")),
                campus = Campus(id = 1, name = "서울"),
            )
        whenever(readingRoomRepository.findById(1)).thenReturn(Optional.of(existingRoom))
        whenever(readingRoomRepository.save(existingRoom)).thenReturn(existingRoom)

        val updatedRoom =
            readingRoomService.updateReadingRoom(
                1,
                UpdateReadingRoomRequest(
                    campusID = 1,
                    name = "열람실 A 수정",
                    total = 20,
                    active = 15,
                    isActive = true,
                    isReservable = true,
                ),
            )
        assertEquals("열람실 A 수정", updatedRoom.name)
        assertEquals(1, updatedRoom.id)
    }

    @Test
    @DisplayName("열람실 ID로 수정 실패 테스트 (존재하지 않는 ID)")
    fun testUpdateReadingRoomNotFound() {
        whenever(readingRoomRepository.findById(999)).thenReturn(Optional.empty())
        assertThrows<ReadingRoomNotFoundException> {
            readingRoomService.updateReadingRoom(
                999,
                UpdateReadingRoomRequest(
                    campusID = 1,
                    name = "열람실 A 수정",
                    total = 20,
                    active = 15,
                    isActive = true,
                    isReservable = true,
                ),
            )
        }
    }

    @Test
    @DisplayName("열람실 ID로 삭제 테스트")
    fun testDeleteReadingRoomById() {
        val room =
            ReadingRoom(
                id = 1,
                name = "열람실 A",
                campusID = 1,
                isActive = true,
                isReservable = true,
                total = 10,
                active = 10,
                occupied = 0,
                updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul")),
                campus = Campus(id = 1, name = "서울"),
            )
        whenever(readingRoomRepository.findById(1)).thenReturn(Optional.of(room))
        readingRoomService.deleteReadingRoomById(1)
        // Verify that the delete method was called
        verify(readingRoomRepository).delete(room)
    }

    @Test
    @DisplayName("열람실 ID로 삭제 실패 테스트 (존재하지 않는 ID)")
    fun testDeleteReadingRoomByIdNotFound() {
        whenever(readingRoomRepository.findById(999)).thenReturn(Optional.empty())
        assertThrows<ReadingRoomNotFoundException> { readingRoomService.deleteReadingRoomById(999) }
    }

    @Test
    @DisplayName("열람실 생성 테스트")
    fun testCreateReadingRoom() {
        val newRoom =
            CreateReadingRoomRequest(
                id = 999,
                campusID = 1,
                name = "열람실 C",
                total = 30,
            )
        // Mock ZonedDateTime.now() to return a fixed time
        whenever(readingRoomRepository.findById(999)).thenReturn(Optional.empty())
        whenever(
            readingRoomRepository.save(
                any(),
            ),
        ).thenReturn(
            ReadingRoom(
                id = 999,
                name = "열람실 C",
                campusID = 1,
                isActive = true,
                isReservable = true,
                total = 30,
                active = 30,
                occupied = 0,
                updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul")),
                campus = null,
            ),
        )
        val createdRoom = readingRoomService.createReadingRoom(newRoom)
        assertEquals("열람실 C", createdRoom.name)
        assertEquals(999, createdRoom.id)
    }

    @Test
    @DisplayName("열람실 생성 실패 테스트 (중복 ID)")
    fun testCreateCampusDuplicateReadingRoomID() {
        val existingRoom =
            ReadingRoom(
                id = 1,
                name = "열람실 A",
                campusID = 1,
                isActive = true,
                isReservable = true,
                total = 10,
                active = 10,
                occupied = 0,
                updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul")),
                campus = Campus(id = 1, name = "서울"),
            )
        whenever(readingRoomRepository.findById(1)).thenReturn(Optional.of(existingRoom))
        val newRoom =
            CreateReadingRoomRequest(
                id = 1,
                campusID = 1,
                name = "열람실 A",
                total = 20,
            )
        assertThrows<DuplicateReadingRoomException> { readingRoomService.createReadingRoom(newRoom) }
    }
}
