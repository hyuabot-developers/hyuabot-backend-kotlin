package app.hyuabot.backend.cafeteria

import app.hyuabot.backend.cafeteria.domain.CreateCafeteriaRequest
import app.hyuabot.backend.cafeteria.domain.UpdateCafeteriaRequest
import app.hyuabot.backend.cafeteria.exception.CafeteriaNotFoundException
import app.hyuabot.backend.cafeteria.exception.DuplicateCafeteriaIDException
import app.hyuabot.backend.campus.exception.CampusNotFoundException
import app.hyuabot.backend.database.entity.Cafeteria
import app.hyuabot.backend.database.repository.CafeteriaRepository
import app.hyuabot.backend.database.repository.CampusRepository
import org.springframework.stereotype.Service

@Service
class CafeteriaService(
    private val campusRepository: CampusRepository,
    private val cafeteriaRepository: CafeteriaRepository,
) {
    fun getCafeteriaList(
        campusID: Int? = null,
        name: String? = null,
    ): List<Cafeteria> =
        when {
            campusID != null && name != null -> cafeteriaRepository.findByCampusIDAndNameContaining(campusID, name)
            campusID != null -> cafeteriaRepository.findByCampusID(campusID)
            name != null -> cafeteriaRepository.findByNameContaining(name)
            else -> cafeteriaRepository.findAll()
        }.sortedBy { it.id }

    fun getCafeteriaById(id: Int): Cafeteria = cafeteriaRepository.findById(id).orElseThrow { CafeteriaNotFoundException() }

    fun createCafeteria(payload: CreateCafeteriaRequest): Cafeteria {
        if (cafeteriaRepository.existsById(payload.id)) {
            throw DuplicateCafeteriaIDException()
        }
        if (!campusRepository.existsById(payload.campusID)) {
            throw CampusNotFoundException()
        }
        return cafeteriaRepository.save(
            Cafeteria(
                id = payload.id,
                campusID = payload.campusID,
                name = payload.name,
                latitude = payload.latitude,
                longitude = payload.longitude,
                breakfastTime = payload.breakfastTime,
                lunchTime = payload.lunchTime,
                dinnerTime = payload.dinnerTime,
                campus = null,
            ),
        )
    }

    fun updateCafeteria(
        id: Int,
        payload: UpdateCafeteriaRequest,
    ): Cafeteria {
        cafeteriaRepository.findById(id).orElseThrow { CafeteriaNotFoundException() }.let { cafeteria ->
            if (!campusRepository.existsById(payload.campusID)) {
                throw CampusNotFoundException()
            }
            cafeteria.apply {
                campusID = payload.campusID
                name = payload.name
                latitude = payload.latitude
                longitude = payload.longitude
                breakfastTime = payload.breakfastTime
                lunchTime = payload.lunchTime
                dinnerTime = payload.dinnerTime
            }
            return cafeteriaRepository.save(cafeteria)
        }
    }

    fun deleteCafeteriaById(id: Int) {
        if (!cafeteriaRepository.existsById(id)) {
            throw CafeteriaNotFoundException()
        }
        cafeteriaRepository.deleteById(id)
    }
}
