package app.hyuabot.backend.database.entity

import app.hyuabot.backend.database.key.ShuttlePeriodID
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity(name = "shuttle_period")
@Table(name = "shuttle_period")
@IdClass(ShuttlePeriodID::class)
data class ShuttlePeriod(
    @Id
    @Column(name = "period_type", nullable = false)
    val type: String,
    @Id
    @Column(name = "period_start", columnDefinition = "timestamptz", nullable = false)
    val start: ZonedDateTime,
    @Id
    @Column(name = "period_end", columnDefinition = "timestamptz", nullable = false)
    val end: ZonedDateTime,
    @ManyToOne
    @JoinColumn(name = "period_type", referencedColumnName = "period_type", insertable = false, updatable = false)
    val periodType: ShuttlePeriodType?,
)
