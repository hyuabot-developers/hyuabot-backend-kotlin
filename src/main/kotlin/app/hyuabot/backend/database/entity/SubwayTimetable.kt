package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalTime

@Entity(name = "subway_timetable")
@Table(name = "subway_timetable")
data class SubwayTimetable(
    @Id
    @Column(name = "station_id", length = 10, nullable = false)
    val stationID: String,
    @Column(name = "start_station_id", length = 10, nullable = false)
    val startStationID: String,
    @Column(name = "terminal_station_id", length = 10, nullable = false)
    val terminalStationID: String,
    @Id
    @Column(name = "departure_time", columnDefinition = "timetz", nullable = false)
    val departureTime: LocalTime,
    @Id
    @Column(name = "weekday", length = 10, nullable = false)
    val weekday: String,
    @Id
    @Column(name = "up_down_type", length = 10, nullable = false)
    val heading: String,
    @ManyToOne
    @JoinColumn(name = "station_id", referencedColumnName = "station_id", insertable = false, updatable = false)
    val station: SubwayRouteStation,
    @OneToOne
    @JoinColumn(name = "start_station_id", referencedColumnName = "station_id", insertable = false, updatable = false)
    val startStation: SubwayRouteStation,
    @OneToOne
    @JoinColumn(name = "terminal_station_id", referencedColumnName = "station_id", insertable = false, updatable = false)
    val terminalStation: SubwayRouteStation,
)
