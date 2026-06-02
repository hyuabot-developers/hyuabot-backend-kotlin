package app.hyuabot.backend.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity(name = "bus_stop")
@Table(name = "bus_stop")
class BusStop(
    @Id
    @Column(name = "stop_id", columnDefinition = "integer", nullable = false)
    val id: Int,
    @Column(name = "stop_name", length = 30, nullable = false)
    var name: String,
    @Column(name = "district_code", columnDefinition = "integer", nullable = false)
    var districtCode: Int,
    @Column(name = "mobile_number", length = 15, nullable = false)
    var mobileNumber: String,
    @Column(name = "region_name", length = 10, nullable = false)
    var regionName: String,
    @Column(name = "latitude", columnDefinition = "double precision", nullable = false)
    var latitude: Double,
    @Column(name = "longitude", columnDefinition = "double precision", nullable = false)
    var longitude: Double,
    @OneToMany(mappedBy = "stop")
    val busRoutes: MutableList<BusRouteStop>,
    @OneToMany(mappedBy = "startStop")
    val startBusRoutes: MutableList<BusRouteStop>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as BusStop
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
