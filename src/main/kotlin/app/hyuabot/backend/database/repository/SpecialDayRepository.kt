package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.SpecialDay
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface SpecialDayRepository : JpaRepository<SpecialDay, Int> {
    fun findByDayDateBetweenOrderByDayDateAscDayNameAsc(
        start: LocalDate,
        end: LocalDate,
    ): List<SpecialDay>
}
