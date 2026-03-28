package app.hyuabot.backend.building

import app.hyuabot.backend.codegen.types.Building
import app.hyuabot.backend.codegen.types.BuildingInput
import app.hyuabot.backend.codegen.types.Room
import app.hyuabot.backend.codegen.types.RoomInput
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument

@DgsComponent
class BuildingDataFetcher(
    private val buildingService: BuildingService,
) {
    @DgsQuery
    fun building(
        @InputArgument buildingInput: BuildingInput?,
        @InputArgument roomInput: RoomInput?,
    ): List<Building> =
        buildingService.fetchBuildings(buildingInput).map {
            Building(
                seq = it.id,
                name = it.name,
                latitude = it.latitude,
                longitude = it.longitude,
                url = it.url,
                rooms =
                    it.room
                        .sortedBy { room ->
                            room.number
                        }.filter { room ->
                            if (roomInput?.name?.isNotEmpty() == true) {
                                room.name.contains(roomInput.name)
                            } else {
                                true
                            }
                        }.map { room ->
                            Room(
                                seq = room.seq!!,
                                name = room.name,
                                number = room.number,
                            )
                        },
            )
        }
}
