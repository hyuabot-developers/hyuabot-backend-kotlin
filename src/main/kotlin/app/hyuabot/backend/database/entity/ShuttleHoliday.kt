package app.hyuabot.backend.database.entity

import app.hyuabot.backend.database.key.ShuttleHolidayID
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.time.LocalDate

@Entity(name = "shuttle_holiday")
@Table(name = "shuttle_holiday")
@IdClass(ShuttleHolidayID::class)
data class ShuttleHoliday(
    @Id
    @Column(name = "holiday_date", columnDefinition = "date", nullable = false)
    val date: LocalDate,
    @Id
    @Column(name = "holiday_type", length = 15, nullable = false)
    val type: String,
    @Id
    @Column(name = "calendar_type", length = 15, nullable = false)
    val calendarType: String,
)
