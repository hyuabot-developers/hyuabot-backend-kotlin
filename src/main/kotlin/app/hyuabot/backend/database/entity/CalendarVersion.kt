package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.ZonedDateTime

@Entity(name = "calendar_version")
@Table(name = "academic_calendar_version")
@SequenceGenerator(name = "academic_calendar_version_version_id_seq", allocationSize = 1)
class CalendarVersion(
    @Id
    @Column(name = "version_id", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "academic_calendar_version_version_id_seq")
    val id: Int? = null,
    @Column(name = "version_name", length = 30, nullable = false)
    var name: String,
    @Column(name = "created_at", columnDefinition = "timestamptz", nullable = false)
    var createdAt: ZonedDateTime,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as CalendarVersion
        return id != null && id == other.id
    }

    override fun hashCode(): Int = Hibernate.getClass(this).hashCode()
}
