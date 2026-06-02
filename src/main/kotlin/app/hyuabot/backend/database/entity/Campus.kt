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
class Campus(
    @Id
    @Column(name = "campus_id", columnDefinition = "integer", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "campus_campus_id_seq")
    val id: Int? = null,
    @Column(name = "campus_name", length = 30, nullable = false)
    var name: String,
    @OneToMany(mappedBy = "campus")
    val building: MutableList<Building> = mutableListOf(),
    @OneToMany(mappedBy = "campus")
    val cafeteria: MutableList<Cafeteria> = mutableListOf(),
    @OneToMany(mappedBy = "campus")
    val readingRoom: MutableList<ReadingRoom> = mutableListOf(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as Campus
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
