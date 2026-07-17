package app.hyuabot.backend.database.repository

import app.hyuabot.backend.database.entity.SubwayTimetable
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime

interface SubwayTimetableRepository : JpaRepository<SubwayTimetable, Int> {
    fun findByStationID(stationID: String): List<SubwayTimetable>

    fun findByStationIDIn(stationIDs: List<String>): List<SubwayTimetable>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT timetable FROM subway_timetable timetable WHERE timetable.stationID IN :stationIDs")
    fun findByStationIDInForUpdate(stationIDs: List<String>): List<SubwayTimetable>

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

    @Query(
        """
            SELECT t from subway_timetable t
            JOIN FETCH t.startStation ss
            JOIN fetch ss.route
            JOIN FETCH t.terminalStation ts
            JOIN FETCH ts.route
            WHERE t.stationID IN :stationIDList
            AND t.heading IN :directions
            AND t.weekday IN :weekdayList
            ORDER BY t.stationID, t.heading, t.weekday, t.departureTime
        """,
    )
    fun findByStationIdInAndHeadingInAndWeekdayIn(
        stationIDList: List<String>,
        directions: List<String>,
        weekdayList: List<String>,
    ): List<SubwayTimetable>

    fun findByStationIDAndHeadingAndWeekdayAndDepartureTimeAfter(
        stationID: String,
        heading: String,
        weekday: String,
        departureTime: LocalTime,
    ): List<SubwayTimetable>

    fun findByStationIDAndHeadingIsInAndWeekdayAndDepartureTimeAfter(
        stationID: String,
        heading: List<String>,
        weekday: String,
        departureTime: LocalTime,
    ): List<SubwayTimetable>

    fun findByStationIDAndHeadingIsInAndWeekdayAndDepartureTimeBefore(
        stationID: String,
        heading: List<String>,
        weekday: String,
        departureTime: LocalTime,
    ): List<SubwayTimetable>

    fun findByStationIDAndHeadingAndWeekdayAndDepartureTime(
        stationID: String,
        heading: String,
        weekday: String,
        departureTime: LocalTime,
    ): SubwayTimetable?

    @Transactional
    fun deleteAllBySeqIn(seqList: List<Int>)

    @Transactional
    fun deleteAllByStationIDIn(stationIDs: List<String>)

    @Transactional
    fun deleteAllByStationIDInAndHeading(
        stationIDs: List<String>,
        heading: String,
    )

    @Transactional
    fun deleteAllByStationIDInAndWeekday(
        stationIDs: List<String>,
        weekday: String,
    )

    @Transactional
    fun deleteAllByStationIDInAndHeadingAndWeekday(
        stationIDs: List<String>,
        heading: String,
        weekday: String,
    )
}
