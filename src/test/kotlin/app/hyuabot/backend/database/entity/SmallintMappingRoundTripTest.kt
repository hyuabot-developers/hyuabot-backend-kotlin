package app.hyuabot.backend.database.entity

import app.hyuabot.backend.database.key.BusLocationID
import app.hyuabot.backend.database.key.BusRealtimeID
import app.hyuabot.backend.database.key.SubwayRealtimeID
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource("classpath:application-test.properties")
class SmallintMappingRoundTripTest(
    @Autowired private val entityManager: EntityManager,
) {
    @Test
    fun `smallint integer fields round trip through postgres`() {
        val updatedAt = ZonedDateTime.parse("2026-01-01T00:00:00Z")
        val busRouteID = 2_000_000_001
        val busStopID = 2_000_000_002
        val busPlateNumber = "SMALLINT-TEST-1"

        val busStop =
            BusStop(
                id = busStopID,
                name = "Smallint Test Stop",
                districtCode = 1,
                mobileNumber = "0000000000",
                regionName = "Test",
                latitude = 0.0,
                longitude = 0.0,
                busRoutes = mutableListOf(),
                startBusRoutes = mutableListOf(),
            )
        entityManager.persist(busStop)

        val busRoute =
            BusRoute(
                id = busRouteID,
                name = "Smallint Test Route",
                typeCode = "TEST",
                typeName = "Test",
                startStopID = busStopID,
                endStopID = busStopID,
                upFirstTime = LocalTime.MIDNIGHT,
                upLastTime = LocalTime.of(23, 59, 59),
                downFirstTime = LocalTime.MIDNIGHT,
                downLastTime = LocalTime.of(23, 59, 59),
                districtCode = 1,
                companyID = 1,
                companyName = "Test",
                companyPhone = "0000000000",
                stop = mutableListOf(),
                startStop = busStop,
                endStop = busStop,
            )
        entityManager.persist(busRoute)
        entityManager.flush()

        entityManager.persist(
            BusRouteStop(
                routeID = busRouteID,
                stopID = busStopID,
                order = 1,
                startStopID = busStopID,
                minuteFromStart = 0,
                route = busRoute,
                stop = busStop,
                startStop = busStop,
            ),
        )
        entityManager.flush()

        val busLocation =
            BusLocation(
                routeID = busRouteID,
                plateNumber = busPlateNumber,
                stationSeq = 1,
                stationID = busStopID,
                updatedAt = updatedAt,
            )
        busLocation.crowded = 17
        busLocation.stateCode = 23
        entityManager.persist(busLocation)

        val busRealtime =
            BusRealtime(
                routeID = busRouteID,
                stopID = busStopID,
                order = 1,
                remainingStop = 1,
                remainingSeat = 2,
                remainingTime = Duration.ZERO,
                isLowFloor = false,
                updatedAt = updatedAt,
            )
        busRealtime.crowded = 31
        busRealtime.stateCode = 37
        entityManager.persist(busRealtime)

        val subwayRouteID = 2_000_000_003
        val subwayStationName =
            SubwayStation(
                name = "Smallint Test Station",
                subwayLine = mutableListOf(),
            )
        val subwayTerminalStationName =
            SubwayStation(
                name = "Smallint Test Terminal",
                subwayLine = mutableListOf(),
            )
        entityManager.persist(subwayStationName)
        entityManager.persist(subwayTerminalStationName)
        val subwayRoute =
            SubwayRoute(
                id = subwayRouteID,
                name = "Smallint Test Route",
                station = mutableListOf(),
            )
        entityManager.persist(subwayRoute)
        entityManager.flush()

        val station =
            subwayRouteStation(
                id = "TST000001",
                name = "Smallint Test Station",
                order = 1,
                routeID = subwayRouteID,
                route = subwayRoute,
                stationName = subwayStationName,
            )
        val terminalStation =
            subwayRouteStation(
                id = "TST000002",
                name = "Smallint Test Terminal",
                order = 2,
                routeID = subwayRouteID,
                route = subwayRoute,
                stationName = subwayTerminalStationName,
            )
        entityManager.persist(station)
        entityManager.persist(terminalStation)
        entityManager.flush()

        val subwayRealtime =
            SubwayRealtime(
                stationID = station.id,
                heading = "UP",
                order = 1,
                location = "Smallint Test Station",
                remainingStop = 1,
                remainingTime = Duration.ZERO,
                terminalStationID = terminalStation.id,
                trainNumber = "TEST-1",
                updatedAt = updatedAt,
                isExpress = false,
                isLast = false,
                status = 1,
                station = station,
                terminalStation = terminalStation,
            )
        subwayRealtime.arrivalCode = 41
        entityManager.persist(subwayRealtime)

        entityManager.flush()
        entityManager.clear()

        val storedBusLocation =
            assertNotNull(
                entityManager.find(BusLocation::class.java, BusLocationID(busRouteID, busPlateNumber)),
            )
        val storedBusRealtime =
            assertNotNull(
                entityManager.find(BusRealtime::class.java, BusRealtimeID(busRouteID, busStopID, 1)),
            )
        val storedSubwayRealtime =
            assertNotNull(
                entityManager.find(SubwayRealtime::class.java, SubwayRealtimeID(station.id, "UP", 1)),
            )

        assertEquals(17, storedBusLocation.crowded)
        assertEquals(23, storedBusLocation.stateCode)
        assertEquals(31, storedBusRealtime.crowded)
        assertEquals(37, storedBusRealtime.stateCode)
        assertEquals(41, storedSubwayRealtime.arrivalCode)
        // @DataJpaTest rolls this transaction back so the fixture never persists in the shared test DB.
    }

    private fun subwayRouteStation(
        id: String,
        name: String,
        order: Int,
        routeID: Int,
        route: SubwayRoute,
        stationName: SubwayStation,
    ) = SubwayRouteStation(
        id = id,
        routeID = routeID,
        name = name,
        order = order,
        cumulativeTime = Duration.ZERO,
        route = route,
        stationName = stationName,
        realtime = mutableListOf(),
        timetable = mutableListOf(),
    )
}
