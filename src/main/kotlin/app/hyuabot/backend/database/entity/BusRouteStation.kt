package app.hyuabot.backend.database.entity

import app.hyuabot.backend.database.key.BusRouteStationID
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity(name = "bus_route_station")
@Table(name = "bus_route_station")
@IdClass(BusRouteStationID::class)
class BusRouteStation(
    @Id
    @Column(name = "route_id", columnDefinition = "integer", nullable = false)
    var routeID: Int,
    @Id
    @Column(name = "station_seq", nullable = false)
    var stationSeq: Int,
    @Column(name = "station_id", nullable = false)
    var stationID: Int,
    @Column(name = "station_name", length = 50, nullable = false)
    var stationName: String,
    @Column(name = "updated_at", columnDefinition = "timestamptz", nullable = false)
    var updatedAt: ZonedDateTime,
)
