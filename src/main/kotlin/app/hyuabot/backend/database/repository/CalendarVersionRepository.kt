package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.CalendarVersion
import org.springframework.data.jpa.repository.JpaRepository

interface CalendarVersionRepository : JpaRepository<CalendarVersion, Int>
