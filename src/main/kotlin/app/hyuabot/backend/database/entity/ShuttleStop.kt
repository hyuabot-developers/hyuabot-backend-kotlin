package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate

@Entity(name = "shuttle_stop")
@Table(name = "shuttle_stop")
class ShuttleStop(
    @Id
    @Column(name = "stop_name", length = 15, nullable = false)
    val name: String,
    @Column(name = "latitude", columnDefinition = "double precision", nullable = false)
    var latitude: Double,
    @Column(name = "longitude", columnDefinition = "double precision", nullable = false)
    var longitude: Double,
    @OneToMany(mappedBy = "stop")
    val route: MutableList<ShuttleRouteStop>,
    @OneToMany(mappedBy = "startStop")
    val routeToStart: MutableList<ShuttleRoute>,
    @OneToMany(mappedBy = "endStop")
    val routeToEnd: MutableList<ShuttleRoute>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as ShuttleStop
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
}
