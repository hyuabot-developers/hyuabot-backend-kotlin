package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
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
    val id: Int? = null,
    @Column(name = "category_id", columnDefinition = "integer", nullable = false)
    var categoryID: Int,
    @Column(name = "title", length = 100, nullable = false)
    var title: String,
    @Column(name = "description", columnDefinition = "text", nullable = false)
    var description: String,
    @Column(name = "start_date", columnDefinition = "date", nullable = false)
    var start: LocalDate,
    @Column(name = "end_date", columnDefinition = "date", nullable = false)
    var end: LocalDate,
    @ManyToOne
    @JoinColumn(name = "category_id", referencedColumnName = "category_id", insertable = false, updatable = false)
    val category: CalendarCategory? = null,
)
