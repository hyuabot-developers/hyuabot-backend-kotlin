package app.hyuabot.backend.building

import app.hyuabot.backend.building.domain.RoomRequest
import app.hyuabot.backend.building.exception.BuildingNotFoundException
import app.hyuabot.backend.building.exception.DuplicateRoomException
import app.hyuabot.backend.building.exception.RoomNotFoundException
import app.hyuabot.backend.database.entity.Room
import app.hyuabot.backend.database.repository.BuildingRepository
import app.hyuabot.backend.database.repository.RoomRepository
import org.springframework.stereotype.Service

@Service
class RoomService(
    private val buildingRepository: BuildingRepository,
    private val roomRepository: RoomRepository,
) {
    fun getRoomList(
        buildingName: String,
        name: String?,
    ): List<Room> =
        when {
            name != null -> roomRepository.findByBuildingNameAndNameContaining(buildingName, name)
            else -> roomRepository.findByBuildingName(buildingName)
        }.sortedBy { it.seq }

    fun getRoomByID(
        buildingName: String,
        seq: Int,
    ): Room =
        roomRepository.findByBuildingNameAndSeq(buildingName, seq)
            ?: throw RoomNotFoundException()

    fun createRoom(
        buildingName: String,
        payload: RoomRequest,
    ): Room {
        buildingRepository.findByName(buildingName)
            ?: throw BuildingNotFoundException()
        roomRepository
            .findByBuildingNameAndNumber(
                buildingName = buildingName,
                number = payload.number,
            )?.let {
                throw DuplicateRoomException()
            }
        return roomRepository.save(
            Room(
                seq = null,
                buildingName = buildingName,
                number = payload.number,
                name = payload.name,
            ),
        )
    }

    fun updateRoom(
        buildingName: String,
        seq: Int,
        payload: RoomRequest,
    ): Room =
        roomRepository.findByBuildingNameAndSeq(buildingName, seq)?.let { room ->
            room.number = payload.number
            room.name = payload.name
            roomRepository.save(room)
        } ?: run {
            throw RoomNotFoundException()
        }

    fun deleteRoom(
        buildingName: String,
        seq: Int,
    ) {
        roomRepository.findByBuildingNameAndSeq(buildingName, seq)?.let {
            roomRepository.delete(it)
        } ?: run {
            throw RoomNotFoundException()
        }
    }
}
