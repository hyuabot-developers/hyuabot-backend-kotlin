package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalTime

@Entity(name = "bus_timetable")
@Table(name = "bus_timetable")
data class BusTimetable(
    @Id
    @Column(name = "route_id", columnDefinition = "integer", nullable = false)
    val routeID: Int,
    @Id
    @Column(name = "start_stop_id", columnDefinition = "integer", nullable = false)
    val startStopID: Int,
    @Id
    @Column(name = "weekday", length = 10, nullable = false)
    val weekday: String,
    @Id
    @Column(name = "departure_time", columnDefinition = "timetz", nullable = false)
    val departureTime: LocalTime,
)
