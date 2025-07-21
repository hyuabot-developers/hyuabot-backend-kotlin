package app.hyuabot.backend.campus

import app.hyuabot.backend.campus.domain.CreateCampusRequest
import app.hyuabot.backend.campus.exception.CampusNotFoundException
import app.hyuabot.backend.campus.exception.DuplicateCampusException
import app.hyuabot.backend.database.entity.Campus
import app.hyuabot.backend.database.repository.CampusRepository
import org.springframework.stereotype.Service

@Service
class CampusService(
    private val campusRepository: CampusRepository,
) {
    fun getCampusList(): List<Campus> = campusRepository.findAll().sortedBy { it.id }

    fun createCampus(payload: CreateCampusRequest): Campus {
        // 캠퍼스 이름 중복 확인
        campusRepository.findByName(payload.name).let {
            if (it.isNotEmpty()) {
                throw DuplicateCampusException()
            }
        }
        val campus = Campus(name = payload.name)
        return campusRepository.save(campus)
    }

    fun getCampusById(id: Int): Campus = campusRepository.findById(id).orElseThrow { CampusNotFoundException() }

    fun updateCampus(
        id: Int,
        payload: CreateCampusRequest,
    ): Campus {
        campusRepository.findById(id).orElseThrow { CampusNotFoundException() }.let { campus ->
            // 캠퍼스 이름 중복 확인
            campusRepository.findByName(payload.name).let {
                if (it.isNotEmpty() && it[0].id != id) {
                    throw DuplicateCampusException()
                }
            }
            campus.name = payload.name
            return campusRepository.save(campus)
        }
    }

    fun deleteCampusById(id: Int) {
        campusRepository.findById(id).orElseThrow { CampusNotFoundException() }.let { campus ->
            campusRepository.delete(campus)
        }
    }
}
