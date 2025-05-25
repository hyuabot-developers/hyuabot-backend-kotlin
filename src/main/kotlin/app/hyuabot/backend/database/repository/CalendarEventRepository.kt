package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.CalendarEvent
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface CalendarEventRepository : JpaRepository<CalendarEvent, Int> {
    fun findByCategoryID(categoryID: Int): List<CalendarEvent>

    fun findByTitleContaining(name: String): List<CalendarEvent>

    fun findByStartBeforeAndEndAfter(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<CalendarEvent>
}
