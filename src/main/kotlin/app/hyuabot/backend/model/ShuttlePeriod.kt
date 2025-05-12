package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity(name = "shuttle_period")
@Table(name = "shuttle_period")
data class ShuttlePeriod(
    @Id
    @Column(name = "period_type")
    val type: String,
    @Id
    @Column(name = "period_start", columnDefinition = "timestamptz")
    val start: ZonedDateTime,
    @Id
    @Column(name = "period_end", columnDefinition = "timestamptz")
    val end: ZonedDateTime,
)
