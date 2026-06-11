package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.LocalDate
import java.time.LocalTime

@Entity(name = "bus_departure_log")
@Table(
    name = "bus_departure_log",
    indexes = [
        Index(name = "idx_bus_departure_log", columnList = "route_id, stop_id, departure_date, departure_time", unique = true),
    ],
)
@SequenceGenerator(name = "bus_departure_log_seq_seq", allocationSize = 1)
class BusDepartureLog(
    @Id
    @Column(name = "seq", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bus_departure_log_seq_seq")
    val seq: Int? = null,
    @Column(name = "route_id", columnDefinition = "integer", nullable = false)
    var routeID: Int,
    @Column(name = "stop_id", columnDefinition = "integer", nullable = false)
    var stopID: Int,
    @Column(name = "departure_date", columnDefinition = "date", nullable = false)
    var departureDate: LocalDate,
    @Column(name = "departure_time", columnDefinition = "time", nullable = false)
    var departureTime: LocalTime,
    @Column(name = "vehicle_id", length = 20, nullable = false)
    var vehicleID: String,
    @ManyToOne
    @JoinColumns(
        JoinColumn(name = "route_id", referencedColumnName = "route_id", insertable = false, updatable = false),
        JoinColumn(name = "stop_id", referencedColumnName = "stop_id", insertable = false, updatable = false),
    )
    val routeStop: BusRouteStop?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as BusDepartureLog
        return seq != null && seq == other.seq
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
}
