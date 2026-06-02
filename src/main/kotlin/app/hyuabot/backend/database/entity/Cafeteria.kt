package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity(name = "cafeteria")
@Table(name = "restaurant")
class Cafeteria(
    @Id
    @Column(name = "restaurant_id", columnDefinition = "integer", nullable = false)
    val id: Int,
    @Column(name = "campus_id", columnDefinition = "integer", nullable = false)
    var campusID: Int,
    @Column(name = "restaurant_name", length = 50, nullable = false)
    var name: String,
    @Column(name = "latitude", columnDefinition = "double precision", nullable = false)
    var latitude: Double,
    @Column(name = "longitude", columnDefinition = "double precision", nullable = false)
    var longitude: Double,
    @Column(name = "breakfast_time", length = 40, nullable = true)
    var breakfastTime: String?,
    @Column(name = "lunch_time", length = 40, nullable = true)
    var lunchTime: String?,
    @Column(name = "dinner_time", length = 40, nullable = true)
    var dinnerTime: String?,
    @ManyToOne
    @JoinColumn(name = "campus_id", referencedColumnName = "campus_id", insertable = false, updatable = false)
    val campus: Campus?,
    @OneToMany(mappedBy = "cafeteria")
    val menu: MutableList<Menu> = mutableListOf(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as Cafeteria
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
