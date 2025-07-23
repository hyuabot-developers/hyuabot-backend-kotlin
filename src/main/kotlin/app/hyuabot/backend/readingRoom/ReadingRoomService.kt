package app.hyuabot.backend.readingRoom

import app.hyuabot.backend.database.entity.ReadingRoom
import app.hyuabot.backend.database.repository.ReadingRoomRepository
import app.hyuabot.backend.readingRoom.domain.CreateReadingRoomRequest
import app.hyuabot.backend.readingRoom.domain.UpdateReadingRoomRequest
import app.hyuabot.backend.readingRoom.exception.DuplicateReadingRoomException
import app.hyuabot.backend.readingRoom.exception.ReadingRoomNotFoundException
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class ReadingRoomService(
    private val readingRoomRepository: ReadingRoomRepository,
) {
    fun getReadingRoomList(): List<ReadingRoom> = readingRoomRepository.findAll().sortedBy { it.id }

    fun createReadingRoom(payload: CreateReadingRoomRequest): ReadingRoom {
        // Reading Room 이름 중복 확인
        readingRoomRepository.findById(payload.id).let {
            if (it.isPresent) {
                throw DuplicateReadingRoomException()
            }
        }
        return readingRoomRepository.save(
            ReadingRoom(
                id = payload.id,
                name = payload.name,
                campusID = payload.campusID,
                isActive = true,
                isReservable = true,
                total = payload.total,
                active = payload.total,
                occupied = 0,
                available = payload.total,
                updatedAt = ZonedDateTime.now(),
                campus = null,
            ),
        )
    }

    fun getReadingRoomById(id: Int): ReadingRoom = readingRoomRepository.findById(id).orElseThrow { ReadingRoomNotFoundException() }

    fun updateReadingRoom(
        id: Int,
        payload: UpdateReadingRoomRequest,
    ): ReadingRoom {
        readingRoomRepository.findById(id).orElseThrow { ReadingRoomNotFoundException() }.let { readingRoom ->
            readingRoom.apply {
                campusID = payload.campusID
                name = payload.name
                total = payload.total
                active = payload.active
                isActive = payload.isActive
                isReservable = payload.isReservable
            }
            return readingRoomRepository.save(readingRoom)
        }
    }

    fun deleteReadingRoomById(id: Int) {
        readingRoomRepository.findById(id).orElseThrow { ReadingRoomNotFoundException() }.let { readingRoom ->
            readingRoomRepository.delete(readingRoom)
        }
    }
}
