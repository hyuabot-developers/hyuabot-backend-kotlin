package app.hyuabot.backend.model

import io.hypersistence.utils.hibernate.type.interval.PostgreSQLIntervalType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import kotlin.time.Duration

@Entity(name = "shuttle_route_stop")
@Table(name = "shuttle_route_stop")
data class ShuttleRouteStop(
    @Id
    @Column(name = "route_name", length = 15)
    val routeName: String,
    @Id
    @Column(name = "stop_name ", length = 15)
    val stopName: String,
    @Column(name = "stop_order", columnDefinition = "integer")
    val order: Int,
    @Type(PostgreSQLIntervalType::class)
    @Column(name = "cumulative_time", columnDefinition = "interval")
    val cumulativeTime: Duration,
)
