package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate

@Entity(name = "subway_station")
@Table(name = "subway_station")
class SubwayStation(
    @Id
    @Column(name = "station_name", length = 30, nullable = false)
    val name: String,
    @OneToMany(mappedBy = "stationName")
    val subwayLine: MutableList<SubwayRouteStation>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as SubwayStation
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
}
