package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity(name = "bus_stop")
@Table(name = "bus_stop")
data class BusStop(
    @Id
    @Column(name = "stop_id", columnDefinition = "integer", nullable = false)
    val id: Int,
    @Column(name = "stop_name", length = 30, nullable = false)
    val name: String,
    @Column(name = "district_code", columnDefinition = "integer", nullable = false)
    val districtCode: Int,
    @Column(name = "mobile_number", length = 15, nullable = false)
    val mobileNumber: String,
    @Column(name = "region_name", length = 10, nullable = false)
    val regionName: String,
    @Column(name = "latitude", columnDefinition = "double precision", nullable = false)
    val latitude: Double,
    @Column(name = "longitude", columnDefinition = "double precision", nullable = false)
    val longitude: Double,
    @OneToMany(mappedBy = "stop")
    val busRoutes: List<BusRouteStop>,
    @OneToMany(mappedBy = "startStop")
    val startBusRoutes: List<BusRouteStop>,
)
