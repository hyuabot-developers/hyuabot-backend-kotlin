package app.hyuabot.backend.model

import io.hypersistence.utils.hibernate.type.interval.PostgreSQLIntervalType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.time.ZonedDateTime
import kotlin.time.Duration

@Entity(name = "bus_realtime")
@Table(name = "bus_realtime")
data class BusRealtime(
    @Id
    @Column(name = "route_id", columnDefinition = "integer")
    val routeID: Int,
    @Id
    @Column(name = "stop_id", columnDefinition = "integer")
    val stopID: Int,
    @Id
    @Column(name = "arrival_sequence", columnDefinition = "integer")
    val order: Int,
    @Column(name = "remaining_stop_count", columnDefinition = "integer")
    val remainingStop: Int,
    @Column(name = "remaining_seat_count", columnDefinition = "integer")
    val remainingSeat: Int,
    @Type(value = PostgreSQLIntervalType::class)
    @Column(name = "remaining_time", columnDefinition = "interval")
    val remainingTime: Duration,
    @Column(name = "low_plate", columnDefinition = "boolean")
    val isLowFloor: Boolean,
    @Column(name = "last_updated_time", columnDefinition = "timestamptz")
    val updatedAt: ZonedDateTime,
)
