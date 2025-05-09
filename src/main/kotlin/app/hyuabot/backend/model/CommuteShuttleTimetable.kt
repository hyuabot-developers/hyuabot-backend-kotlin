package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalTime

@Entity(name = "commute_shuttle_timetable")
@Table(name = "commute_shuttle_timetable")
data class CommuteShuttleTimetable(
    @Id
    @Column(name = "route_name", length = 15)
    val routeName: String,
    @Id
    @Column(name = "stop_name", length = 50)
    val stopName: String,
    @Column(name = "stop_order", columnDefinition = "integer")
    val order: Int,
    @Column(name = "departure_time", columnDefinition = "timetz")
    val departureTime: LocalTime,
)
