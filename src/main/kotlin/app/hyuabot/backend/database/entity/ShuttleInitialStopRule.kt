package app.hyuabot.backend.database.entity

import app.hyuabot.backend.shuttle.domain.ShuttleGeoPoint
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.type.SqlTypes
import java.time.LocalTime
import java.time.OffsetDateTime

@Entity(name = "shuttle_initial_stop_rule")
@Table(
    name = "shuttle_initial_stop_rule",
    indexes = [
        Index(
            name = "idx_shuttle_initial_stop_rule_active",
            columnList = "enabled, period_type, weekday, priority DESC, seq",
        ),
    ],
)
@SequenceGenerator(name = "shuttle_initial_stop_rule_seq_seq", allocationSize = 1)
class ShuttleInitialStopRule(
    @Id
    @Column(name = "seq", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "shuttle_initial_stop_rule_seq_seq")
    val seq: Int? = null,
    @Column(name = "rule_name", length = 80, nullable = false)
    var name: String,
    @Column(name = "period_type", length = 20, nullable = false)
    var periodType: String,
    @Column(name = "weekday", nullable = false)
    var weekday: Boolean,
    @Column(name = "start_time")
    var startTime: LocalTime?,
    @Column(name = "end_time")
    var endTime: LocalTime?,
    @Column(name = "stop_name", length = 15, nullable = false)
    var stopName: String,
    @Column(name = "priority", nullable = false)
    var priority: Int,
    @Column(name = "enabled", nullable = false)
    var enabled: Boolean,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "polygon", columnDefinition = "jsonb", nullable = false)
    var polygon: List<ShuttleGeoPoint>,
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null,
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as ShuttleInitialStopRule
        return seq != null && seq == other.seq
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
}
