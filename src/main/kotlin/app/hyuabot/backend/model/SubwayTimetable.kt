package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalTime

@Entity(name = "subway_timetable")
@Table(name = "subway_timetable")
data class SubwayTimetable(
    @Id
    @Column(name = "station_id", length = 10)
    val stationID: String,
    @Column(name = "start_station_id", length = 10)
    val startStationID: String,
    @Column(name = "terminal_station_id", length = 10)
    val terminalStationID: String,
    @Id
    @Column(name = "departure_time", columnDefinition = "timetz")
    val departureTime: LocalTime,
    @Id
    @Column(name = "weekday", length = 10)
    val weekday: String,
    @Id
    @Column(name = "up_down_type", length = 10)
    val heading: String,
)
