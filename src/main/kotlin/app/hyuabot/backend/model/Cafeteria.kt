package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "cafeteria")
@Table(name = "restaurant")
data class Cafeteria(
    @Id
    @Column(name = "restaurant_id", columnDefinition = "integer")
    val id: Int,
    @Column(name = "campus_id", columnDefinition = "integer")
    val campusID: Int,
    @Column(name = "restaurant_name", length = 50)
    val name: String,
    @Column(name = "latitude", columnDefinition = "double precision")
    val latitude: Double,
    @Column(name = "longitude", columnDefinition = "double precision")
    val longitude: Double,
    @Column(name = "breakfast_time", length = 40)
    val breakfastTime: String,
    @Column(name = "lunch_time", length = 40)
    val lunchTime: String,
    @Column(name = "dinner_time", length = 40)
    val dinnerTime: String,
)
