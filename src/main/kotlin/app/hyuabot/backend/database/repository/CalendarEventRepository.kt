package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.CalendarCategory
import app.hyuabot.backend.database.entity.CalendarEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface CalendarEventRepository : JpaRepository<CalendarEvent, Int> {
    fun findByCategoryID(categoryID: Int): List<CalendarEvent>

    fun findByTitleContaining(name: String): List<CalendarEvent>

    fun findByStartBeforeAndEndAfter(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<CalendarEvent>

    @Query(
        "SELECT DISTINCT c FROM calendar_category c " +
            "LEFT JOIN calendar_event e ON c.id = e.categoryID WHERE e.start <= :endDate AND e.end >= :startDate",
    )
    fun findCategoriesWithEventsBetween(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<CalendarCategory>
}
