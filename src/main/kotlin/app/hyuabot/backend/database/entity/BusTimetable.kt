package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.LocalTime

@Entity(name = "bus_timetable")
@Table(
    name = "bus_timetable",
    indexes = [
        Index(name = "idx_bus_timetable", columnList = "route_id, start_stop_id, weekday, departure_time", unique = true),
    ],
)
@SequenceGenerator(name = "bus_timetable_seq_seq", allocationSize = 1)
class BusTimetable(
    @Id
    @Column(name = "seq", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bus_timetable_seq_seq")
    val seq: Int? = null,
    @Column(name = "route_id", columnDefinition = "integer", nullable = false)
    var routeID: Int,
    @Column(name = "start_stop_id", columnDefinition = "integer", nullable = false)
    var startStopID: Int,
    @Column(name = "weekday", length = 10, nullable = false)
    var weekday: String,
    @Column(name = "departure_time", columnDefinition = "time", nullable = false)
    var departureTime: LocalTime,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as BusTimetable
        return seq != null && seq == other.seq
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
