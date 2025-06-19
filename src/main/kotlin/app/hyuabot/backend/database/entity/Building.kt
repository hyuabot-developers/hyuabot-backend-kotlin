package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
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
    @OneToMany(mappedBy = "building")
    val room: List<Room> = emptyList(),
    @ManyToOne
    @JoinColumn(name = "campus_id", referencedColumnName = "campus_id", insertable = false, updatable = false)
    val campus: Campus,
)
