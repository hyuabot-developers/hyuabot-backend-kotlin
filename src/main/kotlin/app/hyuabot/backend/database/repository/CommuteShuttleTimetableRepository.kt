package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.CommuteShuttleTimetable
import app.hyuabot.backend.database.key.CommuteShuttleTimetableID
import org.springframework.data.jpa.repository.JpaRepository

interface CommuteShuttleTimetableRepository : JpaRepository<CommuteShuttleTimetable, CommuteShuttleTimetableID> {
    fun findByRouteName(routeName: String): List<CommuteShuttleTimetable>
}
