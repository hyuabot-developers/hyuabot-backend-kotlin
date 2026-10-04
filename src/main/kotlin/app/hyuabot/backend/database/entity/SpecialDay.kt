package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.LocalDate
import java.time.ZonedDateTime

@Entity(name = "special_day")
@Table(name = "special_day")
@SequenceGenerator(name = "special_day_seq_seq", allocationSize = 1)
class SpecialDay(
    @Id
    @Column(name = "seq", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "special_day_seq_seq")
    val seq: Int? = null,
    @Column(name = "day_date", columnDefinition = "date", nullable = false)
    var date: LocalDate,
    @Column(name = "day_name", length = 50, nullable = false)
    var name: String,
    @Column(name = "day_kind", length = 20, nullable = false)
    var kind: String,
    @Column(name = "is_holiday", nullable = false)
    var isHoliday: Boolean,
    @Column(name = "source", length = 20, nullable = false)
    var source: String,
    @Column(name = "updated_at", columnDefinition = "timestamptz", nullable = false)
    var updatedAt: ZonedDateTime,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as SpecialDay
        return seq != null && seq == other.seq
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
}
