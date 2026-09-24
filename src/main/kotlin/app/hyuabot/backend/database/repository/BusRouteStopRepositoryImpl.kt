package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.BusRouteStop
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository

@Repository
class BusRouteStopRepositoryImpl(
    @PersistenceContext private val entityManager: EntityManager,
) : BusRouteStopRepositoryCustom {
    override fun fetchBusRouteStopPairs(keys: Set<Pair<Int, Int>>): List<BusRouteStop> {
        if (keys.isEmpty()) return emptyList()

        // Group only stops belonging to each requested route, avoiding a Cartesian filter.
        val stopsByRoute = keys.groupBy({ it.first }, { it.second }).entries.toList()
        val predicates = stopsByRoute.indices.map { "(rs.routeID = :route$it AND rs.stopID IN :stops$it)" }
        val query =
            entityManager.createQuery(
                """
                SELECT rs FROM bus_route_stop rs
                JOIN FETCH rs.route r
                JOIN FETCH r.startStop
                JOIN FETCH r.endStop
                JOIN FETCH rs.stop
                JOIN FETCH rs.startStop
                WHERE ${predicates.joinToString(" OR ")}
                """.trimIndent(),
                BusRouteStop::class.java,
            )
        stopsByRoute.forEachIndexed { index, (route, stops) ->
            query.setParameter("route$index", route)
            query.setParameter("stops$index", stops)
        }
        return query.resultList
    }
}
