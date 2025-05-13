package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalTime

@Entity(name = "commute_shuttle_timetable")
@Table(name = "commute_shuttle_timetable")
data class CommuteShuttleTimetable(
    @Id
    @Column(name = "route_name", length = 15, nullable = false)
    val routeName: String,
    @Id
    @Column(name = "stop_name", length = 50, nullable = false)
    val stopName: String,
    @Column(name = "stop_order", columnDefinition = "integer", nullable = false)
    val order: Int,
    @Column(name = "departure_time", columnDefinition = "timetz", nullable = false)
    val departureTime: LocalTime,
    @ManyToOne
    @JoinColumn(name = "route_name", referencedColumnName = "route_name", insertable = false, updatable = false)
    val route: CommuteShuttleRoute,
    @ManyToOne
    @JoinColumn(name = "stop_name", referencedColumnName = "stop_name", insertable = false, updatable = false)
    val stop: CommuteShuttleStop,
)
