package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.ShuttlePeriod
import org.springframework.data.jpa.repository.JpaRepository
import java.time.ZonedDateTime

interface ShuttlePeriodRepository : JpaRepository<ShuttlePeriod, ShuttlePeriodID> {
    fun findByStartBeforeAndEndAfter(
        start: ZonedDateTime,
        end: ZonedDateTime,
    ): ShuttlePeriod?
}
