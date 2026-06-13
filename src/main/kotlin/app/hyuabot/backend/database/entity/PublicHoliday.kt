package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.LocalDate

@Entity(name = "public_holiday")
@Table(
    name = "public_holiday",
    indexes = [
        Index(name = "idx_public_holiday_date", columnList = "holiday_date, calendar_type", unique = true),
    ],
)
@SequenceGenerator(name = "public_holiday_seq_seq", allocationSize = 1)
class PublicHoliday(
    @Id
    @Column(name = "seq", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public_holiday_seq_seq")
    val seq: Int? = null,
    @Column(name = "holiday_date", columnDefinition = "date", nullable = false)
    var date: LocalDate,
    @Column(name = "holiday_name", length = 30, nullable = false)
    var name: String,
    @Column(name = "calendar_type", length = 15, nullable = false)
    var calendarType: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as PublicHoliday
        return seq != null && seq == other.seq
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
}
