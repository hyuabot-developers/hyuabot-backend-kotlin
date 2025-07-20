package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table

@Entity(name = "campus")
@Table(name = "campus")
@SequenceGenerator(name = "campus_campus_id_seq", allocationSize = 1)
data class Campus(
    @Id
    @Column(name = "campus_id", columnDefinition = "integer", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "campus_campus_id_seq")
    val id: Int? = null,
    @Column(name = "campus_name", length = 30, nullable = false)
    val name: String,
    @OneToMany(mappedBy = "campus")
    val building: List<Building> = emptyList(),
    @OneToMany(mappedBy = "campus")
    val cafeteria: List<Cafeteria> = emptyList(),
    @OneToMany(mappedBy = "campus")
    val readingRoom: List<ReadingRoom> = emptyList(),
)
