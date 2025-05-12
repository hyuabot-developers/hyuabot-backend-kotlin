package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity(name = "cafeteria")
@Table(name = "restaurant")
data class Cafeteria(
    @Id
    @Column(name = "restaurant_id", columnDefinition = "integer", nullable = false)
    val id: Int,
    @Column(name = "campus_id", columnDefinition = "integer", nullable = false)
    val campusID: Int,
    @Column(name = "restaurant_name", length = 50, nullable = false)
    val name: String,
    @Column(name = "latitude", columnDefinition = "double precision", nullable = false)
    val latitude: Double,
    @Column(name = "longitude", columnDefinition = "double precision", nullable = false)
    val longitude: Double,
    @Column(name = "breakfast_time", length = 40, nullable = false)
    val breakfastTime: String,
    @Column(name = "lunch_time", length = 40, nullable = false)
    val lunchTime: String,
    @Column(name = "dinner_time", length = 40, nullable = false)
    val dinnerTime: String,
    @ManyToOne
    @JoinColumn(name = "campus_id", referencedColumnName = "campus_id", insertable = false, updatable = false)
    val campus: Campus,
    @OneToMany(mappedBy = "cafeteria")
    val menu: List<Menu> = emptyList(),
)
