package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "shuttle_stop")
@Table(name = "shuttle_stop")
data class ShuttleStop(
    @Id
    @Column(name = "stop_name ", length = 15, nullable = false)
    val name: String,
    @Column(name = "latitude", columnDefinition = "double precision", nullable = false)
    val latitude: Double,
    @Column(name = "longitude", columnDefinition = "double precision", nullable = false)
    val longitude: Double,
)
