package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.LocalDate

@Entity(name = "calendar_event")
@Table(name = "academic_calendar")
@SequenceGenerator(name = "academic_calendar_academic_calendar_id_seq", allocationSize = 1)
data class CalendarEvent(
    @Id
    @Column(name = "academic_calendar_id", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "academic_calendar_academic_calendar_id_seq")
    val id: Int,
    @Column(name = "category_id", columnDefinition = "integer")
    val categoryID: Int,
    @Column(name = "title", length = 100)
    val title: String,
    @Column(name = "description", columnDefinition = "text")
    val description: String,
    @Column(name = "start_date", columnDefinition = "date")
    val start: LocalDate,
    @Column(name = "end_date", columnDefinition = "date")
    val end: LocalDate,
)
