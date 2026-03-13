package app.hyuabot.backend.building

import app.hyuabot.backend.codegen.types.Building
import app.hyuabot.backend.codegen.types.BuildingInput
import app.hyuabot.backend.codegen.types.Room
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument

@DgsComponent
class BuildingDataFetcher(
    private val buildingService: BuildingService,
) {
    @DgsQuery
    fun building(
        @InputArgument input: BuildingInput?,
    ): List<Building> =
        buildingService.fetchBuildings(input).map {
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
