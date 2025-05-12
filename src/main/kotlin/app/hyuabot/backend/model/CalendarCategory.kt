package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table

@Entity(name = "calendar_category")
@Table(name = "academic_calendar_category")
@SequenceGenerator(name = "academic_calendar_category_category_id_seq", allocationSize = 1)
data class CalendarCategory(
    @Id
    @Column(name = "category_id", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "academic_calendar_category_category_id_seq")
    val id: Int,
    @Column(name = "category_name", length = 30, nullable = false)
    val name: String,
)
