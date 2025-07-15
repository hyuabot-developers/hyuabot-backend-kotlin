package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.CommuteShuttleTimetable
import org.springframework.data.jpa.repository.JpaRepository

interface CommuteShuttleTimetableRepository : JpaRepository<CommuteShuttleTimetable, Int> {
    fun findByRouteName(routeName: String): List<CommuteShuttleTimetable>
}
