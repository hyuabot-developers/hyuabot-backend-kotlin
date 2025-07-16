package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table

@Entity(name = "bus_route_stop")
@Table(
    name = "bus_route_stop",
    indexes = [
        jakarta.persistence.Index(name = "idx_bus_route_stop", columnList = "route_id, stop_id", unique = true),
    ],
)
@SequenceGenerator(name = "bus_route_stop_seq_seq", allocationSize = 1)
data class BusRouteStop(
    @Id
    @Column(name = "seq", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bus_route_stop_seq_seq")
    val seq: Int? = null,
    @Column(name = "route_id", columnDefinition = "integer", nullable = false)
    val routeID: Int,
    @Column(name = "stop_id", columnDefinition = "integer", nullable = false)
    val stopID: Int,
    @Column(name = "stop_seq", columnDefinition = "integer", nullable = false)
    val order: Int,
    @Column(name = "start_stop_id", columnDefinition = "integer", nullable = false)
    val startStopID: Int,
    @Column(name = "minute_from_start", columnDefinition = "integer", nullable = false)
    val minuteFromStart: Int,
    @ManyToOne
    @JoinColumn(name = "route_id", insertable = false, updatable = false)
    val route: BusRoute,
    @ManyToOne
    @JoinColumn(name = "stop_id", insertable = false, updatable = false)
    val stop: BusStop,
    @ManyToOne
    @JoinColumn(name = "start_stop_id", insertable = false, updatable = false)
    val startStop: BusStop,
    @OneToMany(mappedBy = "routeStop")
    val log: List<BusDepartureLog>,
    @OneToMany(mappedBy = "routeStop")
    val realtime: List<BusRealtime>,
)
