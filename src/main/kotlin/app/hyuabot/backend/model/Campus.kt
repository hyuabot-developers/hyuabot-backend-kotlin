package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "campus")
@Table(name = "campus")
data class Campus(
    @Id
    @Column(name = "campus_id", columnDefinition = "integer", nullable = false)
    val id: Int,
    @Column(name = "campus_name", length = 30, nullable = false)
    val name: String,
)
