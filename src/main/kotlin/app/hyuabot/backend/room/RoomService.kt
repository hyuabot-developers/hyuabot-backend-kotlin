package app.hyuabot.backend.room

import app.hyuabot.backend.database.entity.Room
import app.hyuabot.backend.database.repository.RoomRepository
import app.hyuabot.backend.room.domain.RoomRequest
import app.hyuabot.backend.room.exception.DuplicateRoomException
import app.hyuabot.backend.room.exception.RoomNotFoundException
import org.springframework.stereotype.Service

@Service
class RoomService(
    private val roomRepository: RoomRepository,
) {
    fun getRoomList(buildingName: String?): List<Room> =
        buildingName?.let {
            roomRepository.findByNameContaining(it)
        } ?: roomRepository.findAll()

    fun getRoomByID(seq: Int): Room = roomRepository.findById(seq).orElseThrow { RoomNotFoundException() }

    fun createRoom(payload: RoomRequest): Room {
        roomRepository
            .findByBuildingNameAndNumber(
                buildingName = payload.buildingName,
                number = payload.number,
            )?.let {
                throw DuplicateRoomException()
            }
        return roomRepository.save(
            Room(
                seq = null,
                buildingName = payload.buildingName,
                number = payload.number,
                name = payload.name,
            ),
        )
    }

    fun updateRoom(
        seq: Int,
        payload: RoomRequest,
    ): Room =
        roomRepository.findById(seq).orElseThrow { RoomNotFoundException() }.let { room ->
            room.buildingName = payload.buildingName
            room.number = payload.number
            room.name = payload.name
            roomRepository.save(room)
        }

    fun deleteRoom(seq: Int) {
        roomRepository.findById(seq).orElseThrow { RoomNotFoundException() }.let { room ->
            roomRepository.delete(room)
        }
    }
}
