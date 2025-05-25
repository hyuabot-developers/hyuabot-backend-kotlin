package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.CalendarCategory
import org.springframework.data.jpa.repository.JpaRepository

interface CalendarCategoryRepository : JpaRepository<CalendarCategory, Int> {
    fun findByNameContaining(name: String): List<CalendarCategory>
}
