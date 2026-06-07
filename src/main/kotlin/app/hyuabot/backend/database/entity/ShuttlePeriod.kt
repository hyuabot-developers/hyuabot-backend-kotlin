package app.hyuabot.backend.database.entity

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
import java.time.ZonedDateTime

@Entity(name = "shuttle_period")
@Table(
    name = "shuttle_period",
    indexes = [
        Index(name = "idx_shuttle_period_type", columnList = "period_type, period_start, period_end", unique = true),
    ],
)
@SequenceGenerator(name = "shuttle_period_seq_seq", allocationSize = 1)
class ShuttlePeriod(
    @Id
    @Column(name = "seq", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "shuttle_period_seq_seq")
    val seq: Int? = null,
    @Column(name = "period_type", nullable = false)
    var type: String,
    @Column(name = "period_start", columnDefinition = "timestamptz", nullable = false)
    var start: ZonedDateTime,
    @Column(name = "period_end", columnDefinition = "timestamptz", nullable = false)
    var end: ZonedDateTime,
    @ManyToOne
    @JoinColumn(name = "period_type", referencedColumnName = "period_type", insertable = false, updatable = false)
    val periodType: ShuttlePeriodType?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as ShuttlePeriod
        return seq != null && seq == other.seq
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
}
