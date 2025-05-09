package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity(name = "calendar_version")
@Table(name = "academic_calendar_version")
@SequenceGenerator(name = "academic_calendar_version_version_id_seq", allocationSize = 1)
data class CalendarVersion(
    @Id
    @Column(name = "version_id", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "academic_calendar_version_version_id_seq")
    val id: Int,
    @Column(name = "version_name", length = 30)
    val name: String,
    @Column(name = "created_at", columnDefinition = "timestamptz")
    val createdAt: ZonedDateTime,
)
