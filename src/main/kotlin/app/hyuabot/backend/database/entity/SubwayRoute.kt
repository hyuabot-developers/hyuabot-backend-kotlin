package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity(name = "subway_route")
@Table(name = "subway_route")
class SubwayRoute(
    @Id
    @Column(name = "route_id", columnDefinition = "integer", nullable = false)
    val id: Int,
    @Column(name = "route_name", length = 30, nullable = false)
    var name: String,
    @OneToMany(mappedBy = "route")
    val station: MutableList<SubwayRouteStation>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as SubwayRoute
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
