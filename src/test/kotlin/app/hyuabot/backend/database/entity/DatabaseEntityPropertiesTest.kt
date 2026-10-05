package app.hyuabot.backend.database.entity

import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DatabaseEntityPropertiesTest {
    private val now = ZonedDateTime.parse("2026-10-05T12:00:00+09:00[Asia/Seoul]")

    @Test
    fun `bus entities expose their mapped properties`() {
        val location = BusLocation(routeID = 10, plateNumber = "plate", stationSeq = 3, stationID = 20, updatedAt = now)
        location.routeID = 11
        location.plateNumber = "updated-plate"
        location.stationSeq = 4
        location.stationID = 21
        location.remainingSeatCount = 12
        location.lowFloor = true
        location.updatedAt = now.plusMinutes(1)
        assertEquals(11, location.routeID)
        assertEquals("updated-plate", location.plateNumber)
        assertEquals(4, location.stationSeq)
        assertEquals(21, location.stationID)
        assertEquals(12, location.remainingSeatCount)
        assertEquals(true, location.lowFloor)
        assertEquals(now.plusMinutes(1), location.updatedAt)

        val routeStation = BusRouteStation(10, 2, 20, "station", now)
        routeStation.routeID = 11
        routeStation.stationSeq = 3
        routeStation.stationID = 21
        routeStation.stationName = "updated station"
        routeStation.updatedAt = now.plusMinutes(2)
        assertEquals(11, routeStation.routeID)
        assertEquals(3, routeStation.stationSeq)
        assertEquals(21, routeStation.stationID)
        assertEquals("updated station", routeStation.stationName)
        assertEquals(now.plusMinutes(2), routeStation.updatedAt)
        assertTrue(BusRouteStation::class.java.getDeclaredConstructor().newInstance() is BusRouteStation)

        val realtime =
            BusRealtime(
                routeID = 10,
                stopID = 20,
                order = 1,
                remainingStop = 2,
                remainingSeat = 3,
                remainingTime = Duration.ofMinutes(4),
                isLowFloor = false,
                updatedAt = now,
            )
        realtime.currentStopName = "current station"
        realtime.plateNumber = "bus-plate"
        assertEquals("current station", realtime.currentStopName)
        assertEquals("bus-plate", realtime.plateNumber)
    }

    @Test
    fun `subway and schedule entities expose their mapped properties`() {
        val realtime =
            SubwayRealtime(
                stationID = "station",
                heading = "up",
                order = 1,
                location = "location",
                remainingStop = 2,
                remainingTime = Duration.ofMinutes(3),
                terminalStationID = "terminal",
                trainNumber = "train",
                updatedAt = now,
                isExpress = false,
                isLast = false,
                status = 1,
                station = null,
                terminalStation = null,
            )
        realtime.arrivalMessage = "message"
        realtime.arrivalMessageDetail = "detail"
        realtime.remainingSeconds = 17
        assertEquals("message", realtime.arrivalMessage)
        assertEquals("detail", realtime.arrivalMessageDetail)
        assertEquals(17, realtime.remainingSeconds)

        val holiday = PublicHoliday(date = LocalDate.of(2026, 10, 5), name = "holiday", calendarType = "SOLAR")
        holiday.source = "API"
        assertEquals("API", holiday.source)
    }

    @Test
    fun `reading room message is mutable`() {
        val readingRoom =
            ReadingRoom(
                id = 1,
                name = "room",
                campusID = 1,
                isActive = true,
                isReservable = true,
                total = 10,
                active = 8,
                occupied = 3,
                updatedAt = now,
                campus = null,
            )

        readingRoom.unableMessage = "temporarily unavailable"

        assertEquals("temporarily unavailable", readingRoom.unableMessage)
    }

    @Test
    fun `facility equality follows its nullable database identity`() {
        val facility = SubwayStationFacility(7, "station", "elevator", 1, "1", "from", "to", "ko", "en", "source")
        val equalFacility = SubwayStationFacility(7, "other", "other", 9, source = "other")
        val differentFacility = SubwayStationFacility(8, "station", "elevator", 1, source = "source")
        val unsavedFacility = SubwayStationFacility(stationID = "station", facilityType = "elevator", sortOrder = 1, source = "source")
        val otherUnsavedFacility = SubwayStationFacility(stationID = "station", facilityType = "elevator", sortOrder = 1, source = "source")

        assertTrue(facility == facility)
        assertTrue(facility == equalFacility)
        assertFalse(facility == differentFacility)
        assertFalse(facility.equals(null))
        assertFalse(facility.equals("not a facility"))
        assertFalse(unsavedFacility == otherUnsavedFacility)
        assertEquals(7, facility.seq)
        assertEquals("station", facility.stationID)
        assertEquals("elevator", facility.facilityType)
        assertEquals(1, facility.sortOrder)
        assertEquals("1", facility.exitNumber)
        assertEquals("from", facility.fromPlace)
        assertEquals("to", facility.toPlace)
        assertEquals("ko", facility.descriptionKorean)
        assertEquals("en", facility.descriptionEnglish)
        assertEquals("source", facility.source)
        facility.stationID = "updated station"
        facility.facilityType = "stairs"
        facility.sortOrder = 2
        facility.exitNumber = "2"
        facility.fromPlace = "updated from"
        facility.toPlace = "updated to"
        facility.descriptionKorean = "updated ko"
        facility.descriptionEnglish = "updated en"
        facility.source = "updated source"
        assertEquals("updated station", facility.stationID)
        assertEquals("stairs", facility.facilityType)
        assertEquals(2, facility.sortOrder)
        assertEquals("2", facility.exitNumber)
        assertEquals("updated from", facility.fromPlace)
        assertEquals("updated to", facility.toPlace)
        assertEquals("updated ko", facility.descriptionKorean)
        assertEquals("updated en", facility.descriptionEnglish)
        assertEquals("updated source", facility.source)
        assertTrue(facility.hashCode() == facility.hashCode())
    }
}
