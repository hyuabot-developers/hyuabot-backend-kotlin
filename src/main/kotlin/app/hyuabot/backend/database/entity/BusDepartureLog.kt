package app.hyuabot.backend.database.entity

import app.hyuabot.backend.database.key.BusDepartureLogID
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalTime

@Entity(name = "bus_departure_log")
@Table(name = "bus_departure_log")
@IdClass(BusDepartureLogID::class)
data class BusDepartureLog(
    @Id
    @Column(name = "route_id", columnDefinition = "integer", nullable = false)
    val routeID: Int,
    @Id
    @Column(name = "stop_id", columnDefinition = "integer", nullable = false)
    val stopID: Int,
    @Id
    @Column(name = "departure_date", columnDefinition = "date", nullable = false)
    val departureDate: LocalDate,
    @Id
    @Column(name = "departure_time", columnDefinition = "time", nullable = false)
    val departureTime: LocalTime,
    @Column(name = "vehicle_id", length = 20, nullable = false)
    val vehicleID: String,
    @ManyToOne
    @JoinColumns(
        JoinColumn(name = "route_id", referencedColumnName = "route_id", insertable = false, updatable = false),
        JoinColumn(name = "stop_id", referencedColumnName = "stop_id", insertable = false, updatable = false),
    )
    val routeStop: BusRouteStop,
)
