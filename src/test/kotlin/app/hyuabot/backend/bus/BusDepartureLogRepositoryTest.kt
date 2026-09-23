package app.hyuabot.backend.bus

import app.hyuabot.backend.bus.domain.BusDepartureLogKey
import app.hyuabot.backend.database.entity.BusDepartureLog
import app.hyuabot.backend.database.repository.BusDepartureLogRepositoryImpl
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BusDepartureLogRepositoryTest {
    private val entityManager = mock<EntityManager>()
    private val repository = BusDepartureLogRepositoryImpl(entityManager)

    @Test
    fun `keys without dates do not query the database`() {
        assertTrue(repository.findByRouteStopAndDepartureDates(setOf(BusDepartureLogKey(1, 1, emptyList(), 3))).isEmpty())
        verifyNoInteractions(entityManager)
    }

    @Test
    fun `limited keys rank per key in the requested date order and return shared rows once`() {
        val query = mock<Query>()
        val sql = argumentCaptor<String>()
        whenever(entityManager.createNativeQuery(sql.capture(), eq(BusDepartureLog::class.java))).thenReturn(query)
        val shared = log(seq = 1, date = LocalDate.of(2026, 3, 2))
        val other = log(seq = 2, date = LocalDate.of(2026, 3, 1))
        // Hibernate returns one row per matching key, so a row picked by two aliases appears twice.
        whenever(query.resultList).thenReturn(listOf(shared, other, shared))
        val newest = LocalDate.of(2026, 3, 2)
        val oldest = LocalDate.of(2026, 3, 1)

        val result =
            repository.findByRouteStopAndDepartureDates(
                linkedSetOf(
                    BusDepartureLogKey(1, 1, listOf(newest, oldest), 2),
                    BusDepartureLogKey(1, 1, listOf(newest), 1),
                ),
            )

        assertEquals(listOf(shared, other), result)
        assertTrue(sql.firstValue.contains("PARTITION BY k.key_index"))
        assertTrue(sql.firstValue.contains("WHEN 0 THEN CASE b.departure_date WHEN :date0_0 THEN 0 WHEN :date0_1 THEN 1 END"))
        assertTrue(sql.firstValue.contains("WHEN 1 THEN CASE b.departure_date WHEN :date1_0 THEN 0 END"))
        assertTrue(sql.firstValue.contains("(VALUES (0, :route0, :stop0, :limit0), (1, :route1, :stop1, :limit1))"))
        verify(query).setParameter("date0_0", newest)
        verify(query).setParameter("date0_1", oldest)
        verify(query).setParameter("date1_0", newest)
        verify(query).setParameter("limit0", 2)
        verify(query).setParameter("limit1", 1)
    }

    private fun log(
        seq: Int,
        date: LocalDate,
    ) = BusDepartureLog(
        seq = seq,
        routeID = 1,
        stopID = 1,
        departureDate = date,
        departureTime = LocalTime.of(5, 0),
        vehicleID = "v$seq",
    )
}
