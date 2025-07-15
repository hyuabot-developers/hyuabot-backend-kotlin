package app.hyuabot.backend.database.entity

import app.hyuabot.backend.database.key.ShuttleRouteStopID
import io.hypersistence.utils.hibernate.type.interval.PostgreSQLIntervalType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.time.Duration

@Entity(name = "shuttle_route_stop")
@Table(name = "shuttle_route_stop")
@IdClass(ShuttleRouteStopID::class)
data class ShuttleRouteStop(
    @Id
    @Column(name = "route_name", length = 15, nullable = false)
    val routeName: String,
    @Id
    @Column(name = "stop_name ", length = 15, nullable = false)
    val stopName: String,
    @Column(name = "stop_order", columnDefinition = "integer", nullable = false)
    val order: Int,
    @Type(PostgreSQLIntervalType::class)
    @Column(name = "cumulative_time", columnDefinition = "interval", nullable = false)
    val cumulativeTime: Duration,
    @ManyToOne
    @JoinColumn(name = "route_name", referencedColumnName = "route_name", insertable = false, updatable = false)
    val route: ShuttleRoute,
    @ManyToOne
    @JoinColumn(name = "stop_name", referencedColumnName = "stop_name", insertable = false, updatable = false)
    val stop: ShuttleStop,
)
