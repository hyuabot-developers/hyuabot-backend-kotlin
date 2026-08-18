package app.hyuabot.backend.database.repository

import app.hyuabot.backend.bus.domain.BusDepartureLogKey
import app.hyuabot.backend.database.entity.BusDepartureLog
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository

@Repository
class BusDepartureLogRepositoryImpl(
    @PersistenceContext private val entityManager: EntityManager,
) : BusDepartureLogRepositoryCustom {
    override fun findByRouteStopAndDepartureDates(keys: Set<BusDepartureLogKey>): List<BusDepartureLog> {
        val effectiveKeys = keys.filter { it.dates.isNotEmpty() }
        if (effectiveKeys.isEmpty()) return emptyList()

        val criteriaBuilder = entityManager.criteriaBuilder
        val query = criteriaBuilder.createQuery(BusDepartureLog::class.java)
        val root = query.from(BusDepartureLog::class.java)
        val predicates =
            effectiveKeys.map { key ->
                criteriaBuilder.and(
                    criteriaBuilder.equal(root.get<Int>("routeID"), key.routeID),
                    criteriaBuilder.equal(root.get<Int>("stopID"), key.stopID),
                    root.get<java.time.LocalDate>("departureDate").`in`(key.dates),
                )
            }
        query.select(root).where(criteriaBuilder.or(*predicates.toTypedArray()))
        return entityManager.createQuery(query).resultList
    }
}
