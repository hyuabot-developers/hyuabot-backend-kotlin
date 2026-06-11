package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate

@Entity(name = "shuttle_period_type")
@Table(name = "shuttle_period_type")
class ShuttlePeriodType(
    @Id
    @Column(name = "period_type", length = 20, nullable = false)
    val type: String,
    @OneToMany(mappedBy = "periodType")
    val period: MutableList<ShuttlePeriod>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as ShuttlePeriodType
        return type == other.type
    }

    override fun hashCode(): Int = type.hashCode()
}
