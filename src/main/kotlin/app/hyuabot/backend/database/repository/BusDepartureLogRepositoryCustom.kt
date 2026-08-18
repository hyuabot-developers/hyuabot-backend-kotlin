package app.hyuabot.backend.database.repository

import app.hyuabot.backend.bus.domain.BusDepartureLogKey
import app.hyuabot.backend.database.entity.BusDepartureLog

interface BusDepartureLogRepositoryCustom {
    fun findByRouteStopAndDepartureDates(keys: Set<BusDepartureLogKey>): List<BusDepartureLog>
}
