package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "subway_route")
@Table(name = "subway_route")
data class SubwayRoute(
    @Id
    @Column(name = "route_id", columnDefinition = "integer")
    val id: Int,
    @Column(name = "route_name", length = 30)
    val name: String,
)
