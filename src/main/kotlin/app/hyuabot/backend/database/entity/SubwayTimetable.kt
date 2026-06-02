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
class SubwayTimetable(
    @Id
    @Column(name = "seq", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subway_timetable_seq_seq")
    val seq: Int? = null,
    @Column(name = "station_id", length = 10, nullable = false)
    var stationID: String,
    @Column(name = "start_station_id", length = 10, nullable = false)
    var startStationID: String,
    @Column(name = "terminal_station_id", length = 10, nullable = false)
    var terminalStationID: String,
    @Column(name = "departure_time", columnDefinition = "time", nullable = false)
    var departureTime: LocalTime,
    @Column(name = "weekday", length = 10, nullable = false)
    var weekday: String,
    @Column(name = "up_down_type", length = 10, nullable = false)
    var heading: String,
    @ManyToOne
    @JoinColumn(name = "station_id", referencedColumnName = "station_id", insertable = false, updatable = false)
    val station: SubwayRouteStation?,
    @OneToOne
    @JoinColumn(name = "start_station_id", referencedColumnName = "station_id", insertable = false, updatable = false)
    val startStation: SubwayRouteStation?,
    @OneToOne
    @JoinColumn(name = "terminal_station_id", referencedColumnName = "station_id", insertable = false, updatable = false)
    val terminalStation: SubwayRouteStation?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as SubwayTimetable
        return seq != null && seq == other.seq
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
