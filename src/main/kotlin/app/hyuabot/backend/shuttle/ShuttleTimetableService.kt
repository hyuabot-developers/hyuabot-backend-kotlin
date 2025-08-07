package app.hyuabot.backend.shuttle

import app.hyuabot.backend.database.entity.ShuttleTimetableView
import app.hyuabot.backend.database.repository.ShuttleTimetableViewRepository
import org.springframework.stereotype.Service

@Service
class ShuttleTimetableService(
    private val shuttleTimetableViewRepository: ShuttleTimetableViewRepository,
) {
    fun getShuttleTimetable(): List<ShuttleTimetableView> = shuttleTimetableViewRepository.findAll().sortedBy { it.seq }
}
