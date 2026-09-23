package app.hyuabot.backend.bus

import app.hyuabot.backend.database.entity.BusRouteStop
import app.hyuabot.backend.database.repository.BusRouteStopRepositoryImpl
import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BusRouteStopRepositoryTest {
    private val entityManager = mock<EntityManager>()
    private val repository = BusRouteStopRepositoryImpl(entityManager)

    @Test
    fun `empty selection does not query the database`() {
        assertTrue(repository.fetchBusRouteStopPairs(emptySet()).isEmpty())
        verifyNoInteractions(entityManager)
    }

    @Test
    fun `query binds only stops selected for each route and fetches terminals`() {
        val query = mock<TypedQuery<BusRouteStop>>()
        val jpql = argumentCaptor<String>()
        whenever(entityManager.createQuery(jpql.capture(), eq(BusRouteStop::class.java))).thenReturn(query)
        val rows = listOf(mock<BusRouteStop>())
        whenever(query.resultList).thenReturn(rows)

        val result = repository.fetchBusRouteStopPairs(linkedSetOf(1 to 10, 1 to 11, 2 to 20))

        assertEquals(rows, result)
        verify(query).setParameter("route0", 1)
        verify(query).setParameter("stops0", listOf(10, 11))
        verify(query).setParameter("route1", 2)
        verify(query).setParameter("stops1", listOf(20))
        val expectedPredicate =
            "(rs.routeID = :route0 AND rs.stopID IN :stops0) OR " +
                "(rs.routeID = :route1 AND rs.stopID IN :stops1)"
        assertTrue(jpql.firstValue.contains(expectedPredicate))
        assertTrue(jpql.firstValue.contains("JOIN FETCH r.startStop"))
        assertTrue(jpql.firstValue.contains("JOIN FETCH r.endStop"))
    }
}
