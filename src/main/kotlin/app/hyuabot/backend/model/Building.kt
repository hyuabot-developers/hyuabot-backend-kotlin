package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "building")
@Table(name = "building")
data class Building(
    @Column(name = "id", length = 15, nullable = true)
    val id: String?,
    @Id
    @Column(name = "name", length = 30, nullable = false)
    val name: String,
    @Column(name = "campus_id", columnDefinition = "integer", nullable = false)
    val campusID: Int,
    @Column(name = "latitude", columnDefinition = "double precision", nullable = false)
    val latitude: Double,
    @Column(name = "longitude", columnDefinition = "double precision", nullable = false)
    val longitude: Double,
    @Column(name = "url", columnDefinition = "text", nullable = false)
    val url: String,
)
