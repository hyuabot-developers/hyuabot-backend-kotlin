package app.hyuabot.backend.building

import app.hyuabot.backend.building.domain.CreateBuildingRequest
import app.hyuabot.backend.building.domain.UpdateBuildingRequest
import app.hyuabot.backend.building.exception.BuildingNotFoundException
import app.hyuabot.backend.building.exception.DuplicateBuildingNameException
import app.hyuabot.backend.database.entity.Building
import app.hyuabot.backend.database.repository.BuildingRepository
import org.springframework.stereotype.Service

@Service
class BuildingService(
    private val buildingRepository: BuildingRepository,
) {
    fun getBuildingList(
        campusID: Int? = null,
        name: String? = null,
    ): List<Building> =
        when {
            campusID != null && name != null -> buildingRepository.findByCampusIDAndNameContaining(campusID, name)
            campusID != null -> buildingRepository.findByCampusID(campusID)
            name != null -> buildingRepository.findByNameContaining(name)
            else -> buildingRepository.findAll()
        }.sortedBy { it.name }

    fun getBuildingByName(name: String): Building =
        buildingRepository.findByName(name)
            ?: throw BuildingNotFoundException()

    fun createBuilding(payload: CreateBuildingRequest) {
        buildingRepository.let {
            it.findByName(payload.name)?.let { throw DuplicateBuildingNameException() }
            it.save(
                Building(
                    id = payload.id,
                    name = payload.name,
                    campusID = payload.campusID,
                    latitude = payload.latitude,
                    longitude = payload.longitude,
                    url = payload.url,
                ),
            )
        }
    }

    fun updateBuilding(
        name: String,
        payload: UpdateBuildingRequest,
    ) {
        buildingRepository.findByName(name)?.let { building ->
            building.apply {
                id = payload.id
                campusID = payload.campusID
                latitude = payload.latitude
                longitude = payload.longitude
                url = payload.url
            }
            buildingRepository.save(building)
        } ?: throw BuildingNotFoundException()
    }

    fun deleteBuildingByName(name: String) {
        buildingRepository.findByName(name)?.let { building ->
            buildingRepository.delete(building)
        } ?: throw BuildingNotFoundException()
    }
}
