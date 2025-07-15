package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.BusDepartureLog
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface BusDepartureLogRepository : JpaRepository<BusDepartureLog, BusDepartureLogID> {
    fun findByRouteIDAndStopID(
        routeID: Int,
        stopID: Int,
    ): List<BusDepartureLog>

    fun findByRouteIDAndStopIDAndDepartureDate(
        routeID: Int,
        stopID: Int,
        departureDate: LocalDate,
    ): List<BusDepartureLog>
}
