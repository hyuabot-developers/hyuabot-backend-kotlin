package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity(name = "shuttle_stop")
@Table(name = "shuttle_stop")
data class ShuttleStop(
    @Id
    @Column(name = "stop_name", length = 15, nullable = false)
    val name: String,
    @Column(name = "latitude", columnDefinition = "double precision", nullable = false)
    var latitude: Double,
    @Column(name = "longitude", columnDefinition = "double precision", nullable = false)
    var longitude: Double,
    @OneToMany(mappedBy = "stop")
    val route: List<ShuttleRouteStop>,
    @OneToMany(mappedBy = "startStop")
    val routeToStart: List<ShuttleRoute>,
    @OneToMany(mappedBy = "endStop")
    val routeToEnd: List<ShuttleRoute>,
)
