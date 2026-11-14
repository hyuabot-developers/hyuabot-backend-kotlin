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

        val limitedKeys = effectiveKeys.filter { it.limit != null || it.after != null }
        val unlimitedKeys = effectiveKeys.filter { it.limit == null && it.after == null }
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
        query
            .select(root)
            .where(criteriaBuilder.or(*predicates.toTypedArray()))
            .orderBy(
                criteriaBuilder.asc(root.get<java.time.LocalDate>("departureDate")),
                criteriaBuilder.asc(root.get<java.time.LocalTime>("departureTime")),
                criteriaBuilder.asc(root.get<Int>("seq")),
            )
        return entityManager.createQuery(query).resultList
    }

    /**
     * Returns, per key, the first `limit` logs in the key's own date order (then departure time), which is the order
     * `BusRouteService.getBusDepartureLogBatch` takes them in. Each key gets its own window partition so aliases
     * requesting the same route/stop with different dates or limits do not share one ranking. Keys with `after` are
     * also routed here so their window filter and post-midnight-last order run in SQL; BusRouteService re-applies them
     * because rows of all keys come back together.
     */
    private fun findLimited(keys: List<BusDepartureLogKey>): List<BusDepartureLog> {
        if (keys.isEmpty()) return emptyList()

        val keyRows = keys.indices.joinToString(", ") { index -> "($index, :route$index, :stop$index, :limit$index)" }
        val dateRank =
            keys
                .mapIndexed { index, key ->
                    val whens = key.dates.indices.joinToString(" ") { dateIndex -> "WHEN :date${index}_$dateIndex THEN $dateIndex" }
                    "WHEN $index THEN CASE b.departure_date $whens END"
                }.joinToString(" ")
        val wrapRank =
            keys.mapIndexedNotNull { index, key ->
                key.after?.let { "WHEN $index THEN CASE WHEN b.departure_time >= :after$index THEN 0 ELSE 1 END" }
            }
        val wrapOrder = if (wrapRank.isEmpty()) "" else "CASE k.key_index ${wrapRank.joinToString(" ")} ELSE 0 END, "
        val predicates =
            keys
                .mapIndexed { index, key ->
                    val window =
                        if (key.after ==
                            null
                        ) {
                            ""
                        } else {
                            " AND (b.departure_time >= :after$index OR b.departure_time < TIME '04:00:00')"
                        }
                    "(k.key_index = $index AND b.departure_date IN (:dates$index)$window)"
                }.joinToString(" OR ")
        val rankedQuery =
            """
            SELECT seq, route_id, stop_id, departure_date, departure_time, vehicle_id
            FROM (
                SELECT b.seq, b.route_id, b.stop_id, b.departure_date, b.departure_time, b.vehicle_id, k.row_limit,
                    ROW_NUMBER() OVER (
                        PARTITION BY k.key_index
                        ORDER BY CASE k.key_index $dateRank END, ${wrapOrder}b.departure_time, b.seq
                    ) AS row_number
                FROM bus_departure_log b
                JOIN (VALUES $keyRows) AS k(key_index, route_id, stop_id, row_limit)
                    ON b.route_id = k.route_id AND b.stop_id = k.stop_id
                WHERE $predicates
            ) ranked
            WHERE row_number <= row_limit
            ORDER BY departure_date, departure_time, seq
            """.trimIndent()
        val query = entityManager.createNativeQuery(rankedQuery, BusDepartureLog::class.java)
        keys.forEachIndexed { index, key ->
            query.setParameter("route$index", key.routeID)
            query.setParameter("stop$index", key.stopID)
            query.setParameter("dates$index", key.dates)
            query.setParameter("limit$index", key.limit ?: Int.MAX_VALUE)
            key.after?.let { query.setParameter("after$index", it) }
            key.dates.forEachIndexed { dateIndex, date -> query.setParameter("date${index}_$dateIndex", date) }
        }
        // A row selected by several keys is returned once per key; callers regroup by route/stop/date.
        return query.resultList.filterIsInstance<BusDepartureLog>().distinctBy { it.seq }
    }
}
