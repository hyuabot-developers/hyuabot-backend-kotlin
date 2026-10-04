package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.SubwayTrainDelay
import app.hyuabot.backend.database.key.SubwayTrainDelayID
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface SubwayTrainDelayRepository : JpaRepository<SubwayTrainDelay, SubwayTrainDelayID> {
    fun findByRunDate(runDate: LocalDate): List<SubwayTrainDelay>

    fun findByRunDateAndTrainNumberIn(
        runDate: LocalDate,
        trainNumbers: Collection<String>,
    ): List<SubwayTrainDelay>
}
