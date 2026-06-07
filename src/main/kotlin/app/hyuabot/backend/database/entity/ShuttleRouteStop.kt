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
import org.hibernate.Hibernate
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
class ShuttleRouteStop(
    @Id
    @Column(name = "seq", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "shuttle_route_stop_seq_seq")
    val seq: Int? = null,
    @Column(name = "route_name", length = 15, nullable = false)
    var routeName: String,
    @Column(name = "stop_name", length = 15, nullable = false)
    var stopName: String,
    @Column(name = "stop_order", columnDefinition = "integer", nullable = false)
    var order: Int,
    @Type(PostgreSQLIntervalType::class)
    @Column(name = "cumulative_time", columnDefinition = "interval", nullable = false)
    var cumulativeTime: Duration,
    @ManyToOne
    @JoinColumn(name = "route_name", referencedColumnName = "route_name", insertable = false, updatable = false)
    val route: ShuttleRoute?,
    @ManyToOne
    @JoinColumn(name = "stop_name", referencedColumnName = "stop_name", insertable = false, updatable = false)
    val stop: ShuttleStop?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as ShuttleRouteStop
        return seq != null && seq == other.seq
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
}
