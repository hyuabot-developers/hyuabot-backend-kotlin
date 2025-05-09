package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "shuttle_period_type")
@Table(name = "shuttle_period_type")
data class ShuttlePeriodType(
    @Id
    @Column(name = "period_type", length = 20)
    val type: String,
)
