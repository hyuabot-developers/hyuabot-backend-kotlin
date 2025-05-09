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
    @Column(name = "name", length = 30)
    val name: String,
    @Column(name = "campus_id", columnDefinition = "integer")
    val campusID: Int,
    @Column(name = "latitude", columnDefinition = "double precision")
    val latitude: Double,
    @Column(name = "longitude", columnDefinition = "double precision")
    val longitude: Double,
    @Column(name = "url", columnDefinition = "text")
    val url: String,
)
