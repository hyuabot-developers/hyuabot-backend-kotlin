package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.LocalTime

@Entity(name = "subway_timetable")
@Table(
    name = "subway_timetable",
    indexes = [
        Index(
            name = "idx_subway_timetable",
            columnList = "station_id, departure_time, weekday, up_down_type",
            unique = true,
        ),
    ],
)
@SequenceGenerator(name = "subway_timetable_seq_seq", allocationSize = 1)
data class SubwayTimetable(
    @Id
    @Column(name = "seq", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subway_timetable_seq_seq")
    val seq: Int,
    @Column(name = "station_id", length = 10, nullable = false)
    val stationID: String,
    @Column(name = "start_station_id", length = 10, nullable = false)
    val startStationID: String,
    @Column(name = "terminal_station_id", length = 10, nullable = false)
    val terminalStationID: String,
    @Column(name = "departure_time", columnDefinition = "time", nullable = false)
    val departureTime: LocalTime,
    @Column(name = "weekday", length = 10, nullable = false)
    val weekday: String,
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
