package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity(name = "campus")
@Table(name = "campus")
data class Campus(
    @Id
    @Column(name = "campus_id", columnDefinition = "integer", nullable = false)
    val id: Int,
    @Column(name = "campus_name", length = 30, nullable = false)
    val name: String,
    @OneToMany(mappedBy = "campus")
    val building: List<Building> = emptyList(),
    @OneToMany(mappedBy = "campus")
    val cafeteria: List<Cafeteria> = emptyList(),
    @OneToMany(mappedBy = "campus")
    val readingRoom: List<ReadingRoom> = emptyList(),
)
