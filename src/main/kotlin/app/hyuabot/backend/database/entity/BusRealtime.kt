package app.hyuabot.backend.database.entity

import app.hyuabot.backend.database.key.BusRealtimeID
import io.hypersistence.utils.hibernate.type.interval.PostgreSQLIntervalType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Objects

@Entity(name = "bus_realtime")
@Table(name = "bus_realtime")
@IdClass(BusRealtimeID::class)
class BusRealtime(
    @Id
    @Column(name = "route_id", columnDefinition = "integer", nullable = false)
    val routeID: Int,
    @Id
    @Column(name = "stop_id", columnDefinition = "integer", nullable = false)
    val stopID: Int,
    @Id
    @Column(name = "arrival_seq", columnDefinition = "integer", nullable = false)
    val order: Int,
    @Column(name = "remaining_stop_count", columnDefinition = "integer", nullable = false)
    var remainingStop: Int,
    @Column(name = "remaining_seat_count", columnDefinition = "integer", nullable = false)
    var remainingSeat: Int,
    @Type(value = PostgreSQLIntervalType::class)
    @Column(name = "remaining_time", columnDefinition = "interval", nullable = false)
    var remainingTime: Duration,
    @Column(name = "low_plate", columnDefinition = "boolean", nullable = false)
    var isLowFloor: Boolean,
    @Column(name = "last_updated_time", columnDefinition = "timestamptz", nullable = false)
    var updatedAt: ZonedDateTime,
    @ManyToOne
    @JoinColumns(
        JoinColumn(name = "route_id", referencedColumnName = "route_id", insertable = false, updatable = false),
        JoinColumn(name = "stop_id", referencedColumnName = "stop_id", insertable = false, updatable = false),
    )
    val routeStop: BusRouteStop?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as BusRealtime
        return routeID == other.routeID &&
            stopID == other.stopID &&
            order == other.order
    }

    override fun hashCode(): Int = Objects.hash(routeID, stopID, order)
}
