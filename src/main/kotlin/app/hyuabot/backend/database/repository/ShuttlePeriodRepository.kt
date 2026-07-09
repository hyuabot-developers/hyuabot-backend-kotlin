package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.ShuttlePeriod
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface ShuttlePeriodRepository : JpaRepository<ShuttlePeriod, Int> {
    fun findFirstByStartBeforeAndEndAfterOrderByStartDesc(
        start: ZonedDateTime,
        end: ZonedDateTime,
    ): ShuttlePeriod?

    fun findByStartBetweenOrderByStartAsc(
        start: ZonedDateTime,
        end: ZonedDateTime,
    ): List<ShuttlePeriod>
}
