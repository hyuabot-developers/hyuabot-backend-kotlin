package app.hyuabot.backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity(name = "bus_route_stop")
@Table(name = "bus_route_stop")
data class BusRouteStop(
    @Id
    @Column(name = "route_id", columnDefinition = "integer", nullable = false)
    val routeID: Int,
    @Id
    @Column(name = "stop_id", columnDefinition = "integer", nullable = false)
    val stopID: Int,
    @Column(name = "stop_sequence", columnDefinition = "integer", nullable = false)
    val order: Int,
    @Column(name = "start_stop_id", columnDefinition = "integer", nullable = false)
    val startStopID: Int,
    @Column(name = "minute_from_start", columnDefinition = "integer", nullable = false)
    val minuteFromStart: Int,
)
