package app.hyuabot.backend.database.entity

import io.hypersistence.utils.hibernate.type.interval.PostgreSQLIntervalType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import java.time.Duration

@Entity(name = "shuttle_route_stop")
@Table(
    name = "shuttle_route_stop",
    indexes = [
        Index(name = "idx_shuttle_route_stop", columnList = "route_name, stop_order", unique = true),
    ],
)
@SequenceGenerator(name = "shuttle_route_stop_seq_seq", allocationSize = 1)
data class ShuttleRouteStop(
    @Id
    @Column(name = "seq", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "shuttle_route_stop_seq_seq")
    val seq: Int,
    @Column(name = "route_name", length = 15, nullable = false)
    val routeName: String,
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
