package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity(name = "commute_shuttle_stop")
@Table(name = "commute_shuttle_stop")
data class CommuteShuttleStop(
    @Id
    @Column(name = "stop_name", length = 50, nullable = false)
    val name: String,
    @Column(name = "description", length = 100, nullable = false)
    val description: String,
    @Column(name = "latitude", columnDefinition = "double precision", nullable = false)
    val latitude: Double,
    @Column(name = "longitude", columnDefinition = "double precision", nullable = false)
    val longitude: Double,
    @OneToMany(mappedBy = "stop")
    val timetable: List<CommuteShuttleTimetable>,
)
