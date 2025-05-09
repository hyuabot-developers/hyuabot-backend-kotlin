package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "commute_shuttle_stop")
@Table(name = "commute_shuttle_stop")
data class CommuteShuttleStop(
    @Id
    @Column(name = "stop_name", length = 50)
    val name: String,
    @Column(name = "description", length = 100)
    val description: String,
    @Column(name = "latitude", columnDefinition = "double precision")
    val latitude: Double,
    @Column(name = "longitude", columnDefinition = "double precision")
    val longitude: Double,
)
