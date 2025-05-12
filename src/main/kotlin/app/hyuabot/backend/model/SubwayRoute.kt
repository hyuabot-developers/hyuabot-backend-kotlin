package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "subway_route")
@Table(name = "subway_route")
data class SubwayRoute(
    @Id
    @Column(name = "route_id", columnDefinition = "integer", nullable = false)
    val id: Int,
    @Column(name = "route_name", length = 30, nullable = false)
    val name: String,
)
