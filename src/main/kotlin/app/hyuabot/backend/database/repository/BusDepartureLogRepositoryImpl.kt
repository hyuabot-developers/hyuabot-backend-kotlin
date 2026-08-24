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

        val limitedKeys = effectiveKeys.filter { it.limit != null }
        val unlimitedKeys = effectiveKeys.filter { it.limit == null }
        return findUnlimited(unlimitedKeys) + findLimited(limitedKeys)
    }

    private fun findUnlimited(keys: List<BusDepartureLogKey>): List<BusDepartureLog> {
        if (keys.isEmpty()) return emptyList()

        val criteriaBuilder = entityManager.criteriaBuilder
        val query = criteriaBuilder.createQuery(BusDepartureLog::class.java)
        val root = query.from(BusDepartureLog::class.java)
        val predicates =
            keys.map { key ->
                criteriaBuilder.and(
                    criteriaBuilder.equal(root.get<Int>("routeID"), key.routeID),
                    criteriaBuilder.equal(root.get<Int>("stopID"), key.stopID),
                    root.get<java.time.LocalDate>("departureDate").`in`(key.dates),
                )
            }
        query.select(root).where(criteriaBuilder.or(*predicates.toTypedArray()))
        return entityManager.createQuery(query).resultList
    }

    private fun findLimited(keys: List<BusDepartureLogKey>): List<BusDepartureLog> {
        if (keys.isEmpty()) return emptyList()

        val predicates =
            keys.mapIndexed { index, key ->
                "(route_id = :route$index AND stop_id = :stop$index AND departure_date IN (:dates$index))"
            }
        val rankedQuery =
            """
            SELECT seq, route_id, stop_id, departure_date, departure_time, vehicle_id
            FROM (
                SELECT b.*, ROW_NUMBER() OVER (
                    PARTITION BY route_id, stop_id
                    ORDER BY departure_date, departure_time, seq
                ) AS row_number
                FROM bus_departure_log b
                WHERE ${predicates.joinToString(" OR ")}
            ) ranked
            WHERE ${keys.mapIndexed { index, _ ->
                "(route_id = :route$index AND stop_id = :stop$index AND row_number <= :limit$index)"
            }.joinToString(" OR ")}
            """.trimIndent()
        val query = entityManager.createNativeQuery(rankedQuery, BusDepartureLog::class.java)
        keys.forEachIndexed { index, key ->
            query.setParameter("route$index", key.routeID)
            query.setParameter("stop$index", key.stopID)
            query.setParameter("dates$index", key.dates)
            query.setParameter("limit$index", key.limit)
        }
        return query.resultList.filterIsInstance<BusDepartureLog>()
    }
}
