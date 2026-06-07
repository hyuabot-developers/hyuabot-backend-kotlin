package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate

@Entity(name = "building")
@Table(name = "building")
class Building(
    @Column(name = "id", length = 15, nullable = true)
    var id: String?,
    @Id
    @Column(name = "name", length = 30, nullable = false)
    val name: String,
    @Column(name = "campus_id", columnDefinition = "integer", nullable = false)
    var campusID: Int,
    @Column(name = "latitude", columnDefinition = "double precision", nullable = false)
    var latitude: Double,
    @Column(name = "longitude", columnDefinition = "double precision", nullable = false)
    var longitude: Double,
    @Column(name = "url", columnDefinition = "text", nullable = true)
    var url: String? = null,
    @OneToMany(mappedBy = "building")
    val room: MutableList<Room> = mutableListOf(),
    @ManyToOne
    @JoinColumn(name = "campus_id", referencedColumnName = "campus_id", insertable = false, updatable = false)
    val campus: Campus? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as Building
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
}
