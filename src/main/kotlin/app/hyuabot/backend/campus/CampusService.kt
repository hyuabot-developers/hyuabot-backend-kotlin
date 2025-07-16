package app.hyuabot.backend.campus

import app.hyuabot.backend.database.entity.Campus
import app.hyuabot.backend.database.repository.CampusRepository
import org.springframework.stereotype.Service

@Service
class CampusService(
    private val campusRepository: CampusRepository,
) {
    fun getCampusList(): List<Campus> = campusRepository.findAll().sortedBy { it.id }
}
