package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.SubwayTimetable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalTime

interface SubwayTimetableRepository : JpaRepository<SubwayTimetable, Int> {
    fun findByStationID(stationID: String): List<SubwayTimetable>

    fun findByStationIDAndDepartureTimeAfter(
        stationID: String,
        departureTime: LocalTime,
    ): List<SubwayTimetable>

    fun findByStationIDAndHeading(
        stationID: String,
        heading: String,
    ): List<SubwayTimetable>

    fun findByStationIDAndDepartureTimeAfterAndHeading(
        stationID: String,
        departureTime: LocalTime,
        heading: String,
    ): List<SubwayTimetable>

    fun findByStationIDAndWeekday(
        stationID: String,
        weekday: String,
    ): List<SubwayTimetable>

    fun findByStationIDAndWeekdayAndDepartureTimeAfter(
        stationID: String,
        weekday: String,
        departureTime: LocalTime,
    ): List<SubwayTimetable>

    fun findByStationIDAndHeadingAndWeekday(
        stationID: String,
        heading: String,
        weekday: String,
    ): List<SubwayTimetable>

    fun findByStationIDAndHeadingAndWeekdayAndDepartureTimeAfter(
        stationID: String,
        heading: String,
        weekday: String,
        departureTime: LocalTime,
    ): List<SubwayTimetable>
}
